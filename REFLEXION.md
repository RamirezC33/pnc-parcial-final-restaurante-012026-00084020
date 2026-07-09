# Reflexión sobre el uso de IA

Para este parcial me apoyé en Claude (Claude Code) como asistente, pero traté de no
tragarme todo lo que me generaba. Acá dejo mi reflexión honesta de cómo fue el proceso.

## 1. Qué generó bien la IA sin que tuviera que corregir

La verdad es que la estructura general salió bastante bien de entrada. La separación en
capas (controller para la parte web, service para la lógica, repository y domain para los
datos) quedó ordenada y con las responsabilidades donde tenían que estar.

Las entidades JPA y sus relaciones (sucursal, mesa, pedido, detalle del pedido, usuario)
también salieron correctas casi a la primera. No tuve que pelearme con los mapeos.

Y en la parte de seguridad, el manejo de las contraseñas con BCrypt y la idea de guardar
el refresh token en la base para poder revocarlo me pareció bien pensada, no tuve que
tocarla.

## 2. Qué se equivocó, sobre todo en seguridad

Hubo un par de cosas que si las dejaba pasar, el proyecto ni siquiera arrancaba:

- El `build.gradle` que venía traía dependencias que directamente no existen
  (`spring-boot-starter-webmvc` y varios starters de test inventados). Eso habría roto la
  compilación completa.
- La IA usó una clase (`AuthenticationConfiguration`) que ya fue eliminada en Spring
  Security 7, que es la versión que trae Spring Boot 4. Otro error de compilación.

En el tema de seguridad puro, el riesgo más grande que hay que vigilar es que la IA
tiende a resolver la autorización solo mirando el rol (por ejemplo con
`hasRole('ENCARGADO')`) y se olvida de la parte de la sucursal. Si me quedaba solo con
eso, cualquier encargado podría meterse a modificar pedidos de otra sucursal, que es
justo lo que la regla de negocio no debe permitir. Por eso la comparación por sucursal la
puse aparte y no confié en que el rol solo alcanzara.

## 3. Cómo detecté los errores y cómo los corregí

Los errores de dependencias y de la clase eliminada los cachaba al compilar. Corría
`./gradlew compileJava` y `./gradlew dependencies` y ahí saltaban. Los arreglé cambiando
las dependencias por las reales y armando el `AuthenticationManager` a mano con
`DaoAuthenticationProvider` y `ProviderManager`, que es como se hace ahora.

Para la regla de negocio no me quedé solo con que compilara: hice una prueba unitaria
(`SucursalAccessGuardTest`) que verifica que un encargado de una sucursal reciba un 403 si
intenta operar sobre otra, y también lo probé a mano levantando todo con Docker y pegándole
a la API con curl. Ahí confirmé que el encargado de la sucursal Norte recibía 403 al tocar
un pedido de la sucursal Centro, y que el de Centro sí lo podía modificar.

## 4. Cómo le explicaría a un compañero la autorización por sucursal

Le diría algo así: tener el rol de encargado no basta para dejarte hacer la operación,
porque todos los encargados tienen el mismo rol. Lo que los diferencia es a qué sucursal
pertenecen.

Cuando el usuario hace login, su sucursal queda guardada dentro del token JWT firmado, así
que el servidor puede confiar en ese dato. Además, cada pedido guarda a qué sucursal
pertenece. Entonces, antes de confirmar, modificar o cancelar un pedido, comparo la
sucursal que viene en el token contra la sucursal del pedido. Si no coinciden y el usuario
es encargado, respondo 403. El administrador se salta esa comparación porque tiene acceso a
todo, y el cliente solo puede tocar sus propios pedidos.

Esa comparación la dejé centralizada en una clase (`SucursalAccessGuard`) para no repetir la
misma lógica en cada lado y que si algún día cambia, se cambie en un solo lugar.
