# kssandra_task

## Descripción
Aplicación Java para la obtención de datos de cotización de las principales criptomonedas y el cálculo de predicciones de precio a corto plazo.<br>
La predición mínima se encuentra en 15min mientras que la máxima es 24h.<br>
Cada predicción es evaluada y asociada a un % de fiabilidad en base a resultados anteriores.<br>
La aplicación recibe como entrada información proveniente de distintos proveedores, obtenida a través de peticiones REST. La información resultante del procesamiento es almacenada en BBDD. Esta se podrá consultar a traves de la API REST del módulo [kassandra_ws](https://github.com/aquesadat/kssandra_ws "kassandra_ws")

## Funcionamiento
Una tarea planificada (Spring Task Scheduler) es ejecutada indefinidamente realizando las siguientes acciones:
1. Obtener proveedor de información activo. Actualmente uno de estos: [Coingecko](https://www.coingecko.com/ "Coingecko") y [AlphaVantage](https://www.alphavantage.co/ "AlphaVantage").
2. Obtener criptomonedas activas (configuradas en BBDD)
3. Mediante un thread pool, de manera concurrente para cada cryptomoneda se realizan las siguientes acciones:
	- Llamada REST para la obtención de datos de cotización actual del proveedor activo. 
	- Cálculo de predicciones en base a la información previamente obtenido y a la ya persistida en BBDD. (módulo [kssandra_core](https://github.com/aquesadat/kssandra_core "kssandra_core"))
	- Evaluación de predicciones anteriores en base a los resultados reales obtenidos a fin de determinar la precicisión de la predicción.
4. Toda la información obtenida en el punto anterior se persiste en una bbdd MySQL (módulo [kssandra_persistence](https://github.com/aquesadat/kssandra_persistence "kssandra_persistence"))
5. Mantenimiento BBDD: Se eliminan todos los registros que por antigüedad ya no intervengan en el cálculo de predicciones.


## Esquema
