/*
 * Run on the 'ncdc' database.
 */
print(ISODate());
db.data.createIndex({'position': '2dsphere'});
print('done');
print(ISODate());
