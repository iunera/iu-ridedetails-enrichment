# iu-ridedetails-enrichment

This project is an assembly of different processors which compute enrichment for ride data.
Furthermore, it transform the data into a uniform data format.
The resulting data stream of tagged GPS points for a vehicle ride are consumed from a Kafka stream and subsequently and then logical additions, required for analytics are added to the data stream and output to a defined Kafka stream.
This way, once can chain the processors or easily use the existing ones as templates.
The following processors are provided:

## Stateful and Stateless processors
There are stateful and stateless processors provided. 
As best practice, we recommend to first execute the stateful processors and the the stateless.
In case processors require generated data form prior processors, we recommend to chain those in on job or to indicate it clearly in the output and input topic names to avoid confusion.

## ComputableKPIEnricher
Computes basic KPIs for each stop:
- The absolute change in passengers (exits+entries)
- The absolute amount of passenger growth (occupancy arrival-exists+entries)
- The retaining guests (occupancy arrival-exists)
- The total stop duration
- The departure delay
- The arrival delay
- Passenger entrance speed (passengers in per second)
- Passenger exit speed (passengers out per second)

## ComputeRouteStateKPIs
Requires the ComputableKPIEnricher to be executed first.
This is a stateful processor what means the events that are consumed and enriched by it need to be in a correct order for each partition (main partition is the line of a vehicle).

In general the values are computed to allow later analytics/machine learning such as:
- How long how many passengers were together in a ride (e.g. "How full was it for how long")
- Predictions such as when X people get in at this stop how many get then out in the next stop and similar

Thus the processor computes and adds the current values to the data for each stop:
- boarding passengers
- exiting passengers
- retaining passengers
- entries at the next stop seen form the current stop
- exits at the next stop seen form the current stop
- retainers at the next stop seen form the current stop
- current stop label
- next stop label
- the natural key of the stop (see the fuzzy key generation in the data types project)
- arrival time at stop
- departure time at the stop before
- duration form the last stop to the current stop 

-> In case further stateful variables depending on the current or last stop are required to e computed this processor serves as a template to add further enrichment.

## DayMetaHourEnricher
Add the hour as extra field that analytics steps at a later point of time can group by the hour more easily. 

## EnrichtmentWeekends
Adds if the data is form a weekend.

## HolidayEnrichment
Adds an indicator if the data is form a holiday. Uses **https://github.com/svendiedrichsen/jollyday** internally. 
Can be extended to detect also more complex holidays.

## RepetitiveEventNaturalKeyEnricher
Generates a natural key for an event such as a stop that is comparable to other stops near the same geoposition.

## ConsolidationEnricher
Shows how results or prior enrichers can be aggregated. Here we combine if it is a weekend or if there are school vacations to derive an indicator if schools were open and therefore pupils in public transport.

## SchoolVacationEnricher
Adds the school holidays for a region based on the https://ferien-api.de/.

## StopFunctionEnricher
Adds the function of the stop. E.g. is it a school or a train station or what can we say be meta information about the place to generate better training vectors.

# Required JVN args
```
--add-opens java.base/java.lang=ALL-UNNAMED 
--add-opens java.base/java.util=ALL-UNNAMED 
--add-opens java.base/java.util.concurrent.atomic=ALL-UNNAMED
```

# License
[Open Compensation Token License, Version 0.20](https://github.com/open-compensation-token-license/license/blob/main/LICENSE.md)
