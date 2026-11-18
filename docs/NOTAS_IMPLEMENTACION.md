# Notas de implementación (es)

Decisiones respecto a la especificación (docs/SPEC.md):

- **Nombre**: *Clean Village* (id `cleanvillage`) en todos los idiomas.
- **Herramientas (H1–H5)**: simplificado a una sola herramienta, la *Esponja jabonosa*, que limpia todo
  (mugre, moho, telarañas, bloques sucios y vidrio empañado). Se usa con clic derecho mantenido o clic izquierdo.
  Cuando se seca, se recarga con clic derecho sobre agua. Receta: lana amarilla + bola de slime.
- **Plataforma (T1)**: estructura común/fabric/neoforge como Architectury, pero compilada con `build.py`
  (sin Gradle) porque Minecraft 26.x no está ofuscado y el entorno de desarrollo no tenía red.
  Forge queda fuera de la v1 (la especificación lo permitía).
- **Pruebas (T8)**: servidor Fabric headless (`tools/harness.py`) + instancia de cliente en
  `.minecraft/pt-tests/cl-26.3-fabric` controlada con un *script runner* que solo existe en la build `--dev`.
- **Pendiente**: probar en NeoForge 26.3 (requiere instalar NeoForge 26.3 en el launcher) y en un servidor
  multijugador real con dos jugadores.
