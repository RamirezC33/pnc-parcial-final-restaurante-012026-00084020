# Bitácora de prompts

Acá voy dejando los prompts que fui usando durante el desarrollo. La herramienta
principal que usé fue Claude (Claude Code). Trato de ser fiel a lo que realmente pregunté,
lo que me generó y, sobre todo, lo que tuve que corregir o rehacer yo. No fue "dame el
proyecto entero": fui haciéndolo por partes, probando y arreglando cuando algo no cerraba.

Nota: este es mi borrador. Si al probar me salen más errores o cambio cosas, los sigo
agregando abajo.

---

## Arranque: entender qué pedía el parcial

**Prompt:** "Analizame este proyecto y el README, decime qué pide exactamente y tené en
cuenta que es de N-Capas. No cambies nada todavía, confirmame primero."

- Me devolvió un resumen de los requisitos (JWT, roles, regla de negocio, Docker, CI/CD y
  los entregables) y me marcó que el `build.gradle` traía dependencias raras.
- Decisión mía: elegí PostgreSQL y la **opción B** (autorización por sucursal). La B me
  convenció porque el enunciado ya habla de una cadena con varias sucursales y de un
  encargado atado a una, entonces la regla sale natural y es la que mejor puedo explicar.

## build.gradle

**Prompt:** "Revisá el build.gradle, hay dependencias que creo que no existen."

- Confirmó que `spring-boot-starter-webmvc` y varios starters `*-test` no existen en Maven
  Central. Los cambió por los reales y agregó Postgres y jjwt.

**Prompt:** "¿Cómo sé que Spring Boot 4.1 y todo esto de verdad resuelve y no me va a
explotar después?"

- Me dijo de correr `./gradlew dependencies`. Lo corrí y ahí confirmé que bajaba todo. Esto
  lo hice antes de escribir código, para no avanzar sobre algo roto.

## Capa de datos

**Prompt:** "Armá las entidades JPA: sucursal, mesa, producto, usuario, pedido y detalle,
con las relaciones."

- Las generó con sus enums de estado.

**Prompt:** "En el pedido, ¿no conviene guardar también la sucursal directo, aunque ya
esté en la mesa? Después la voy a comparar mucho."

- Me explicó que sí, que denormalizar la sucursal en el pedido hace que la comparación de
  la regla B sea directa y no dependa de ir a buscar la mesa. Me pareció bien y lo dejé así.

**Prompt:** "¿Por qué me pusiste EAGER en varias relaciones? ¿No es malo para performance?"

- Me explicó el trade-off. Para el tamaño de este proyecto lo dejé, pero quería entender por
  qué estaba.

## Seguridad y JWT

**Prompt:** "Ahora el login: que devuelva access token de 15 min y refresh de 7 días, con
endpoint para renovar. Explicame por qué el refresh lo guardás en la base y el access no."

- Generó el servicio de JWT, el filtro y la config. La explicación que me dio: el access es
  stateless (se valida solo con la firma) y el refresh se guarda para poder revocarlo antes
  de que expire.

**Error que me tiró:** al compilar me daba error con `AuthenticationConfiguration`.

**Prompt:** "Me sale que no existe `AuthenticationConfiguration`, ¿qué pasó?"

- Resulta que esa clase la sacaron en Spring Security 7 (la que viene con Spring Boot 4). Lo
  arreglamos armando el `AuthenticationManager` a mano con `DaoAuthenticationProvider` y
  `ProviderManager`. Este fue el error más claro de "la IA usó algo viejo".

## Regla de negocio B

**Prompt:** "Necesito que un encargado solo pueda tocar mesas y pedidos de SU sucursal. Y
ojo, no lo resuelvas solo con el rol, porque todos los encargados tienen el mismo rol."

- Sacó la comparación a una clase aparte (`SucursalAccessGuard`) y la usó en los servicios
  de pedidos y mesas. Me gustó que quedara en un solo lugar.

**Prompt:** "Hacé una prueba que demuestre que un encargado de una sucursal recibe 403 si
toca otra."

- Generó el test unitario. Lo corrí con `./gradlew test` y pasó.

**Prompt (después de levantar todo):** "Quiero probarlo de verdad, no solo el test. Pasame
los curl para loguearme como los dos encargados y probar el 403 y el 200."

- Lo probé a mano: el encargado Norte recibió 403 al confirmar un pedido de Centro, y el de
  Centro sí lo pudo confirmar. También vi que el listado le mostraba solo lo de su sucursal.

## Docker

**Prompt:** "Dockerfile y docker-compose con Postgres, que arranque con docker-compose up."

- Hizo el Dockerfile multi-etapa y el compose.

**Error:** al hacer `docker-compose up` me dio "address already in use" en el 8080.

**Prompt:** "Me dice que el puerto 8080 está ocupado, ¿cómo lo pruebo sin matar lo otro?"

- Tenía otra cosa corriendo en el 8080. Para probar lo mapeamos a otro puerto y ahí levantó
  bien.

**Detalle que noté:** `/actuator/health` me daba 401. Faltaba la dependencia de actuator, la
agregamos.

## CI/CD y documentación

**Prompt:** "Workflow de GitHub Actions que compile, corra los tests, falle si hay un
secreto o una vulnerabilidad crítica."

- Armó el workflow con gitleaks (secretos) y Trivy (vulnerabilidades).

**Prompt:** "El README dejámelo del proyecto (cómo levantar, capas, roles, la regla B), pero
guardá aparte la consigna original para no perderla."

- Movió la consigna a `docs/CONSIGNA.md` y escribió el README del proyecto.
