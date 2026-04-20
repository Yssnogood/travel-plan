// Neo4j Initialization Cypher Script
// Creates indexes, constraints, and initial data for travel graph database

// Create constraints for uniqueness
CREATE CONSTRAINT destination_name IF NOT EXISTS FOR (d:Destination) REQUIRE d.id IS UNIQUE;
CREATE CONSTRAINT activity_id IF NOT EXISTS FOR (a:Activity) REQUIRE a.id IS UNIQUE;
CREATE CONSTRAINT travel_id IF NOT EXISTS FOR (t:Travel) REQUIRE t.id IS UNIQUE;
CREATE CONSTRAINT user_id IF NOT EXISTS FOR (u:User) REQUIRE u.id IS UNIQUE;
CREATE CONSTRAINT category_name IF NOT EXISTS FOR (c:Category) REQUIRE c.name IS UNIQUE;

// Create indexes for performance
CREATE INDEX destination_country IF NOT EXISTS FOR (d:Destination) ON (d.country);
CREATE INDEX destination_city IF NOT EXISTS FOR (d:Destination) ON (d.city);
CREATE INDEX activity_category IF NOT EXISTS FOR (a:Activity) ON (a.category);
CREATE INDEX travel_status IF NOT EXISTS FOR (t:Travel) ON (t.status);

// Create activity categories
MERGE (c:Category {name: 'Adventure'}) SET c.description = 'Outdoor and adventure activities';
MERGE (c:Category {name: 'Culture'}) SET c.description = 'Museums, historical sites, and cultural experiences';
MERGE (c:Category {name: 'Food'}) SET c.description = 'Restaurants, food tours, and culinary experiences';
MERGE (c:Category {name: 'Nature'}) SET c.description = 'Parks, hiking, and natural attractions';
MERGE (c:Category {name: 'Entertainment'}) SET c.description = 'Shows, concerts, and entertainment venues';
MERGE (c:Category {name: 'Relaxation'}) SET c.description = 'Spas, beaches, and relaxation activities';
MERGE (c:Category {name: 'Shopping'}) SET c.description = 'Markets, malls, and shopping districts';
MERGE (c:Category {name: 'Sports'}) SET c.description = 'Sports events and recreational activities';

// Create sample destinations
MERGE (paris:Destination {id: 'dest-1', name: 'Paris', country: 'France', city: 'Paris'})
SET paris.latitude = 48.8566, paris.longitude = 2.3522, paris.description = 'The City of Light';

MERGE (london:Destination {id: 'dest-2', name: 'London', country: 'United Kingdom', city: 'London'})
SET london.latitude = 51.5074, london.longitude = -0.1278, london.description = 'Historic capital city';

MERGE (rome:Destination {id: 'dest-3', name: 'Rome', country: 'Italy', city: 'Rome'})
SET rome.latitude = 41.9028, rome.longitude = 12.4964, rome.description = 'The Eternal City';

MERGE (barcelona:Destination {id: 'dest-4', name: 'Barcelona', country: 'Spain', city: 'Barcelona'})
SET barcelona.latitude = 41.3851, barcelona.longitude = 2.1734, barcelona.description = 'Mediterranean gem';

MERGE (amsterdam:Destination {id: 'dest-5', name: 'Amsterdam', country: 'Netherlands', city: 'Amsterdam'})
SET amsterdam.latitude = 52.3676, amsterdam.longitude = 4.9041, amsterdam.description = 'Venice of the North';

// Create relationships between nearby destinations
MATCH (paris:Destination {name: 'Paris'}), (london:Destination {name: 'London'})
MERGE (paris)-[:CONNECTED_TO {distance_km: 344, transport_type: 'train'}]->(london);

MATCH (paris:Destination {name: 'Paris'}), (amsterdam:Destination {name: 'Amsterdam'})
MERGE (paris)-[:CONNECTED_TO {distance_km: 504, transport_type: 'train'}]->(amsterdam);

MATCH (paris:Destination {name: 'Paris'}), (barcelona:Destination {name: 'Barcelona'})
MERGE (paris)-[:CONNECTED_TO {distance_km: 1037, transport_type: 'flight'}]->(barcelona);

MATCH (rome:Destination {name: 'Rome'}), (barcelona:Destination {name: 'Barcelona'})
MERGE (rome)-[:CONNECTED_TO {distance_km: 1362, transport_type: 'flight'}]->(barcelona);

// Create sample activities
MERGE (a1:Activity {id: 'act-1', name: 'Eiffel Tower Visit', category: 'Culture'})
SET a1.duration_hours = 2, a1.price = 25.00, a1.currency = 'EUR';

MERGE (a2:Activity {id: 'act-2', name: 'Louvre Museum', category: 'Culture'})
SET a2.duration_hours = 4, a2.price = 17.00, a2.currency = 'EUR';

MERGE (a3:Activity {id: 'act-3', name: 'Seine River Cruise', category: 'Relaxation'})
SET a3.duration_hours = 1.5, a3.price = 15.00, a3.currency = 'EUR';

MERGE (a4:Activity {id: 'act-4', name: 'British Museum', category: 'Culture'})
SET a4.duration_hours = 3, a4.price = 0.00, a4.currency = 'GBP';

MERGE (a5:Activity {id: 'act-5', name: 'Colosseum Tour', category: 'Culture'})
SET a5.duration_hours = 2.5, a5.price = 22.00, a5.currency = 'EUR';

// Link activities to destinations
MATCH (paris:Destination {name: 'Paris'}), (a:Activity) WHERE a.id IN ['act-1', 'act-2', 'act-3']
MERGE (a)-[:LOCATED_IN]->(paris);

MATCH (london:Destination {name: 'London'}), (a:Activity {id: 'act-4'})
MERGE (a)-[:LOCATED_IN]->(london);

MATCH (rome:Destination {name: 'Rome'}), (a:Activity {id: 'act-5'})
MERGE (a)-[:LOCATED_IN]->(rome);

// Link activities to categories
MATCH (a:Activity), (c:Category) WHERE a.category = c.name
MERGE (a)-[:BELONGS_TO]->(c);
