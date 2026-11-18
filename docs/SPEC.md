# Clean Village — Especificación del mod

Nov 17, 2026 · @Joel

## Resumen

En algunas aldeas aparecen 1–2 casas muy sucias; el jugador las limpia con herramientas que deja el propio aldeano, y al terminar el aldeano celebra con música y le entrega una carta de agradecimiento y una recompensa.

**Nombre:** Clean Village (id `cleanvillage`).

**Experiencia objetivo:** de la tristeza a la alegría en 10–20 minutos. El jugador debe sentir tres cosas, en este orden:

1. **Empatía:** entra en una casa oscura y mugrienta y encuentra al aldeano sentado en la cama, cabizbajo.
2. **Satisfacción:** frotar la suciedad tiene feedback inmediato (partículas, sonido, el bloque recupera su color) y una barra de progreso que sube.
3. **Recompensa emocional:** el aldeano ve su casa limpia, salta de alegría con una melodía y le lanza su carta escrita a mano junto al premio.

**Momento viral:** la secuencia final (casa reluciente, música, aldeano saltando) y los antes/después de la casa son lo que se comparte en clips cortos. Todo el diseño prioriza que esos dos momentos queden muy bien en vídeo.

## Flujo del jugador

El recorrido es lineal y cada paso tiene un estado claro del aldeano (entre corchetes).

1. **Descubrir** \[Triste\]: el jugador ve desde fuera una casa con ventanas sucias, telarañas y musgo. Al entrar, el aldeano está sentado en la cama, cabizbajo, con partículas de lágrima. Suena un ambiente apagado.
2. **Entender** \[Triste\]: en un cofre junto a la cama hay un libro escrito por el aldeano (qué le pasó a su casa) y el kit de limpieza.
3. **Empezar a limpiar** \[Esperando fuera\]: con el primer bloque limpiado aparece la barra de progreso "Casa de Marta: 3%". *Propuesta:* el aldeano se levanta, sale y espera en la puerta, para que después pueda "entrar a ver su casa".
4. **Limpiar** \[Esperando fuera\]: el jugador frota suciedad, quita telarañas, rasca moho. La barra sube; al 50% el aldeano asoma la cabeza y se anima un poco.
5. **Terminar** \[Celebrando\]: al 100% la casa brilla un instante, el aldeano entra, mira alrededor, salta 3–4 veces con partículas de corazón y suena una melodía alegre.
6. **Recompensa** \[Agradecido\]: el aldeano se acerca al jugador y le lanza un libro de agradecimiento firmado, junto con la recompensa.
7. **Después** \[Normal\]: el aldeano vuelve a su vida normal, el jugador tiene el efecto Héroe de la aldea y mejor reputación con el aldeano y la casa queda limpia para siempre.

## Requisitos funcionales

Cada requisito lleva un código para referenciarlo en el plan de trabajo.

### G — Generación de casas sucias

- **G1:** En las aldeas generadas nuevas, 1–2 casas (configurable) se marcan como "sucias". Probabilidad configurable de que una aldea tenga casas sucias (por defecto 60%).
- **G2:** Funciona en los 5 tipos de aldea vanilla (llanura, desierto, sabana, taiga, nevada) reutilizando las casas vanilla: la suciedad se aplica sobre la casa existente, sin estructuras nuevas.
- **G3:** Se ensucian solo casas con cama y al menos \~20 bloques de interior, para que haya trabajo suficiente.
- **G4:** La casa guarda sus límites (caja de la estructura) y su aldeano propietario, de forma persistente.

### S — Sistema de suciedad

- **S1: Capa de mugre:** bloque fino que se pega a cualquier cara (suelo, paredes, techo), como el liquen. 3 niveles de intensidad que bajan con cada frotada.
- **S2: Bloques sucios:** variantes sucias de tablones, piedra y vidrio (vidrio empañado) que vuelven al bloque original al limpiarse.
- **S3: Moho y telarañas:** manchas de moho verde y telarañas distribuidas en esquinas y techos.
- **S4: Feedback:** cada frotada da partículas de burbujas y gotas, un sonido de frotar y un cambio visual inmediato. El bloque limpio brilla un instante.
- **S5: Progreso:** barra en pantalla (estilo barra de jefe) con el nombre del aldeano y el porcentaje limpio, visible solo dentro de la casa.
- **S6:** La suciedad no se regenera y no se puede eliminar rompiendo bloques a mano de forma que el progreso cuente (romper con pico no cuenta: hay que limpiar).

### H — Herramientas de limpieza

- **H1: Esponja jabonosa:** limpia la capa de mugre y los bloques sucios. Durabilidad limitada.
- **H2: Cepillo de fregar:** más rápido en suelos y moho, más lento en paredes.
- **H3: Plumero:** quita telarañas sin romperlas como bloque.
- **H4: Cubo con jabón:** recarga la esponja; se rellena en agua.
- **H5:** Todas se pueden fabricar con recetas sencillas, para usarlas también en tu propia base.

### A — Aldeano propietario

- **A1:** Tiene nombre visible ("Marta", "Tom"...) y no despawnea ni cambia de casa.
- **A2: Triste:** sentado en el borde de la cama, cabeza baja, partículas ocasionales de lágrima. Si el jugador le habla, en vez de comerciar muestra una frase triste.
- **A3: Esperando:** cuando empieza la limpieza sale y espera junto a la puerta; no se aleja más de unos bloques.
- **A4: Protegido:** no puede morir mientras la misión está activa (zombis, caídas), para no romper la historia.

### C — Celebración

- **C1:** Al llegar al 100%, destello breve de toda la casa y sonido de "brillo".
- **C2:** El aldeano entra, gira la cabeza mirando alrededor y salta 3–4 veces con partículas de corazón y de felicidad.
- **C3:** Suena una melodía alegre de 5–8 segundos, audible para jugadores cercanos.
- **C4:** La secuencia dura unos 6–10 segundos y no se puede interrumpir accidentalmente.

### R — Recompensa

- **R1:** El aldeano lanza al jugador un libro escrito y firmado por él, agradeciendo la limpieza.
- **R2:** Lanza también la recompensa material, definida por una loot table configurable. Propuesta: esmeraldas seguras, oro probable y diamantes con probabilidad baja, escalando con el tamaño de la casa. Además, un objeto único decorativo de la familia (por ejemplo, un retrato enmarcado o el delantal del aldeano), con textura generada por Claude.
- **R3:** Todos los jugadores que ayudaron reciben el efecto Héroe de la aldea (nivel y duración configurables; por defecto nivel I durante 40 minutos, como tras defender una incursión), con descuentos en todos los aldeanos y regalos ocasionales. Además mejora su reputación permanente con el aldeano propietario.
- **R4:** Cada casa da la recompensa una sola vez.

## Requisitos técnicos

- **T1 Plataforma:** multiloader desde el inicio (Fabric, Forge y NeoForge) con Architectury, en la última versión estable de Minecraft Java, Java 21. Si Forge tarda en soportar la última versión, se publica primero en Fabric y NeoForge.
- **T2 Arquitectura de datos:** estado de cada casa (límites, propietario, bloques sucios restantes, fase) en datos persistentes del mundo; sobrevive a reinicios y a chunks descargados.
- **T3 Generación:** la suciedad se aplica tras generar la aldea mediante un procesador de estructura o un post-procesado, sin reemplazar archivos vanilla, para ser compatible con otros mods de aldeas.
- **T4 Multijugador:** funciona en servidores. Cualquier jugador puede limpiar; todos los jugadores que hayan limpiado al menos un bloque reciben su propia carta y recompensa. Las animaciones y la música se sincronizan con todos los jugadores cercanos.
- **T5 Rendimiento:** el progreso se calcula por contador, no recorriendo la casa en cada tick. Sin impacto medible en TPS con 10 casas activas.
- **T6 Configuración:** probabilidad por aldea, número de casas, recompensa (loot table de datapack), activar o desactivar la invulnerabilidad del aldeano.
- **T7 Idiomas:** todos los textos en archivos de idioma; castellano e inglés desde la v1.
- **T8 Pruebas:** Claude prueba cada hito en tu ordenador con una instancia de Minecraft aparte (por ejemplo en Prism Launcher), usando un mundo de pruebas con comandos para generar aldeas y teletransportarse.

## Contenido y assets

Claude hace todo el contenido de texto y las texturas base; el sonido es lo único que necesita tu oído.

| Asset | Cantidad v1 | Quién | Notas |
| --- | --- | --- | --- |
| Historias del libro inicial | 6 variantes | Claude | Inundación, viaje largo, enfermedad, incendio en la chimenea, invasión de arañas, ataque de saqueadores |
| Cartas de agradecimiento | 6 variantes (una por historia) | Claude | Firmadas con el nombre del aldeano; tono cálido y algo divertido |
| Frases del aldeano triste | \~10 | Claude | Aparecen al intentar hablar con él |
| Texturas de suciedad | Mugre (3 niveles), moho, vidrio empañado, variantes sucias de bloques | Claude | 16x16 estilo vanilla, iteradas con capturas en el juego |
| Texturas de herramientas | 4 | Claude | Esponja, cepillo, plumero, cubo con jabón |
| Sonidos de frotar y burbujas | 3–4 variantes | Tú revisas | Primera versión con sonidos vanilla modificados (tono y volumen) |
| Melodía de celebración | 1 | Claude compone, tú apruebas | v1: secuencia de notas con sonidos de bloque musical (arpa, campana, flauta). Más adelante, un .ogg propio |
| Ambiente triste | 1 | Tú revisas | Sonido de lluvia lejana o goteo dentro de la casa |

## Plan de trabajo

Siete hitos en orden; cada uno termina con una prueba en el juego y unas capturas para ti antes de pasar al siguiente. Lo más arriesgado (que limpiar sea satisfactorio) va primero.

### Hito 0 — Preparación

- [ ] Proyecto multiloader (Fabric, Forge y NeoForge) que compila y arranca en una instancia de pruebas de cada loader
- [ ] Mundo de pruebas con comandos de depuración (generar casa sucia, saltar fase)

**Aceptación:** el mod carga sin errores y aparece en la lista de mods.

### Hito 1 — Limpiar es satisfactorio (S1, S2, S4, H1–H4)

- [ ] Capa de mugre con 3 niveles y bloques sucios
- [ ] Herramientas con recetas y durabilidad
- [ ] Partículas, sonido y destello al limpiar

**Aceptación:** en una habitación de prueba, limpiar 20 bloques se siente fluido; tú validas el sonido.

### Hito 2 — Casas sucias en aldeas (G1–G4, S3, T3)

- [ ] Selección de 1–2 casas por aldea y aplicación de suciedad
- [ ] Cofre con kit de limpieza y libro
- [ ] Probado en los 5 tipos de aldea

**Aceptación:** en 10 aldeas nuevas, las casas sucias se ven claramente sucias desde fuera y ninguna se rompe.

### Hito 3 — Progreso (S5, S6, T2, T5)

- [ ] Barra de progreso con nombre del aldeano
- [ ] Estado persistente tras salir y volver a entrar al mundo

**Aceptación:** el porcentaje es correcto y sobrevive a un reinicio.

### Hito 4 — El aldeano (A1–A4)

- [ ] Aldeano sentado triste en la cama, con lágrimas
- [ ] Sale a esperar a la puerta al empezar la limpieza
- [ ] Protegido durante la misión

**Aceptación:** capturas del aldeano triste aprobadas por ti; no se escapa ni muere en 30 minutos de prueba.

### Hito 5 — Celebración y recompensa (C1–C4, R1–R4)

- [ ] Secuencia de entrada, saltos y partículas
- [ ] Melodía de celebración
- [ ] Libro de agradecimiento y loot table

**Aceptación:** grabas o miras la secuencia completa y te emociona; esta es la escena del tráiler.

### Hito 6 — Contenido, pulido y lanzamiento (T4, T6, T7)

- [ ] 6 historias y 6 cartas en castellano e inglés
- [ ] Configuración y prueba en servidor multijugador
- [ ] Página de Modrinth y CurseForge, capturas e icono

**Aceptación:** una partida completa de principio a fin sin errores en un jugador y en servidor.

## Alcance, riesgos y preguntas abiertas

**Fuera de la v1:** suciedad que se acumula en las casas del jugador con el tiempo, casas sucias en otras estructuras (mansiones, puestos), clientes recurrentes que piden limpiezas.

**Riesgos técnicos:**

- **El aldeano no tiene postura de sentado:** el modelo vanilla no la tiene. Hay que retocar el renderizado (piernas dobladas, cabeza baja). Es la pieza visual más delicada y se valida con capturas en el hito 4.
- **Elegir 1–2 casas por aldea:** la generación de aldeas va por piezas sueltas; coordinar cuántas se ensucian por aldea requiere un paso posterior a la generación.
- **Pathfinding del aldeano:** que entre a la casa de forma fiable en la celebración (puertas, escaleras, otros mobs en medio). Plan B: teletransporte suave con partículas.

**Decisiones tomadas (17 nov 2026):**

- **Historia del aldeano:** sale a esperar a la puerta al empezar la limpieza y entra a celebrar al terminar.
- **Recompensa:** escalada por tamaño de casa, más un objeto único decorativo; assets generados por Claude.
- **Multijugador:** reciben recompensa todos los que ayudaron.
- **Plataformas:** última versión en Fabric, Forge y NeoForge.
- **Pendiente:** nombre definitivo del mod.
