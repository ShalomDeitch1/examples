---
description: This prompt is used to define what we need to accomplish in this subproject
agent: agent
model: GPT-5.2 (Preview)
---
The repository is organized into subprojects, each demonstrating a specific concept or technology.
The subject is a local delivery service similar to "GoPuff".
There are:
 warehouse that have an inventory of items,
 customers that can place orders for items, 
 orders that the customers place

We need to be able to deliver the items in the order within a certain time frame (one hour).
The user should get a list of items within 100 milliseconds.
The list should only contain items that can be delivered within the time frame.
The system should be able to handle at least 1000 concurrent users placing orders without performance degradation.

Non-functional requirements:
 for listing items: 100 milliseconds response time => availability over consistency
 for placing orders: 2 seconds response time => consistency over availability
  (we will mock payments, so no need to integrate with payment providers)
 
 To implement this, we will need to use geolocation services to track driver locations and the expected time taken to deliver items.
 (We will use a mock service to give the travel time between two locations based on distance.)
 We will give an estimate of what warehouses are relevant for a customer based on their location.
 To reduce the response time for listing items and service costs, we will cache the items that can be delivered to a certain location from relevant warehouses with a TTL of 15 minutes.
 To reduuce the number of distances to check from the expensive geolocation service, we will cluster the customer locations into grids of 1km x 1km. This will help to quickly estimate which warehouses are relevant for a customer based on their location and help with caching.

 To keep things simple, we assume that if the items are in stock in the any warehouse that is close enough, that is part of the order, there is no need to require items are from only one warehouse (even if that is more likely in a real implmentation).

 Mock the payment service, while an order is waiting for payment confirmation, the items should be reserved in the warehouse inventory to avoid overselling.
 
 Out of scope:
  We ignore the drivers, this is because in the example given in [helloInterview](https://www.hellointerview.com/learn/system-design/problem-breakdowns/gopuff), the drivers are not modelled.


  Each concept should be implemented in a separate subproject with its own README.md explaining the concept being demonstrated. 
  Each README.md should contain:
   - Explanation of the concept being demonstrated
   - Architecture diagram using mermaid
   - Instructions on how to run the subproject
   - Any trade-offs or design decisions made

  The main project README.md should give an overview of all the subprojects and their purpose. (It should not go into details about each subproject, nor should it have mermaid diagrams,that is for the individual README.md files.)

  SubProjects:
    1. postgresGeo
       Give an example of how to use the basic geoSpacial functionality using PostgreSQL with PostGIS
       Keep everything simple, this is to help the reader know how to use geospatial queries in PostgreSQL with PostGIS.
       Show use of "prefix" indexing for geospatial queries.

    2. redisGeo
       Give an example of how to use the basic geoSpacial functionality using Redis
       Keep everything simple, this is to help the reader know how to use geospatial queries in Redis.
       Show use of "prefix" indexing for geospatial queries.

    3. cachingWithRedisGeo
       Give an example of how to cache the items that can be delivered to a certain location from relevant warehouses with a very short TTL using Redis.
       Show how to structure the cache keys and values for efficient retrieval and how Redis can be a cache for both postgres Geo queries and external geolocation service.
       Show use of "prefix" indexing for geospatial queries.

    4. To aid accesing the lists of items in warehouses quickly, it can help to use Read duplicates of the database.
      Show how to set up read duplicates using PostgreSQL streaming replication.
      Show how to configure the application to use read duplicates for read-heavy operations like listing items.
      Explain the trade-offs of using read duplicates, such as eventual consistency and increased complexity in database management.

    5. using a cache with read duplicates
       Show how to combine caching with Redis and read duplicates to optimize read performance further.
       Explain the interaction between the cache and read duplicates, including cache invalidation strategies when data changes in the primary database AND versioning the data in the cache to avoid stale reads from read duplicates.  

    6. simpleLocalDeliveryService   
        A simple local delivery service that demonstrates the core functionality of listing items and placing orders.
        Do not implement caching or geolocation optimizations yet.
        Services like how long it takes to deliver items from warehouses to customers can be a mock service with simple distance calculations.
        Focus on getting the basic functionality right with clean code and proper unit tests.

    7. optimizedLocalDeliveryService
       An optimized version of the local delivery service that incorporates caching with Redis and geolocation optimizations and duplicate databases for read-heavy operations + redis cache and versioning.   


   The design should prepare the projects and the README.md for each subproject and a task list of what needs to be done to implement each subproject. The details should be clear enough for a competent developer to implement each subproject based on the README.md and task list.