# ML Experiments

Esta entrega se ha centrado en la parte de aplicacion Android y en dejar preparada la integracion futura del modulo de IA.

## Estado actual

- No se han ejecutado nuevos experimentos de entrenamiento en esta iteracion.
- La app ya genera y guarda los datos necesarios para conectar un motor TFLite mas adelante.
- El motor actual de la app es una capa local basada en reglas para mantener el flujo funcional sin introducir IA real.

## Estructura prevista

- `ml/data/`
  Datos, features y artefactos de preprocesado.
- `ml/models/`
  Modelos exportados y versionados.
- `ml/scripts/`
  Scripts de entrenamiento y exportacion.
- `ml/experiments/` o este propio documento
  Registro de pruebas, metricas y comparativas.

## Cuando se implemente la IA

Este documento debera ampliarse con:

- Scripts utilizados.
- Configuraciones de entrenamiento.
- Dataset y pipeline de preprocesado.
- Metricas obtenidas.
- Comparacion entre modelos.
- Resultado final exportado para Android.
