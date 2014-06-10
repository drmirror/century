/*
 * Run on the 'ncdc' database.
 */
print(ISODate());
print('position 2dsphere index');
db.data.createIndex({ts: 1, position: '2dsphere'});

print('done');
print(ISODate());
