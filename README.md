# kssandra_task

## Descripción
Aplicación Java para obtener datos de cotización de las principales criptomonedas y calcular predicciones de precio a corto plazo en base a estas. 
La predición mínima se encuentra en 15min mientras que la máxima es 24h.<br>
Cada predicción se acompaña de un % de fiabilidad basado en resultados anteriores.
Toda la información procesada por esta aplicación es obtenida vía WS de provedores como AlphaVantage y/o Coingecko y es almacenada en BBDD.

## Funcionamiento
De manera planificada (Spring Task Scheduler) se ejecuta una tarea encargada de realizar las siguientes acciones: 
1. Obtener proveedor de información activo. Actualmente Coingecko y AlphaVantage.
2.  Obtener criptomonedas configuradas. Actualmente: 
3. Mediante un thread pool, de manera concurrente para cada cryptomoneda se realizan las siguientes acciones:
	- Obtención de los datos de cotización actual. 
	- Cálculo de predicciones. (módulo ksd_core)
	- Evaluación de predicciones anteriores en base a los resultados reales obtenidos.
4. Toda la información obtenida en el punto anterior se persiste en una bbdd MySQL (ksd_persistence)



## Esquema
