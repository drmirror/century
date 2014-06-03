/*
 * Convert ncdc.data collection into something flat that Monary can query.
 */

db.data.aggregate([
    {
        $match: {
            ts: {$gt: ISODate("2013-12-01T00:00:00")},
            // Valid temperature samples.
            'airTemperature.quality': '1',
            // Positions of 0, 0 are probably invalid.
            'position.coordinates': {'$ne': [0, 0]}
        }
}, {
    $project: {
        ts: true,
        position: true,
        unwindablePosition: '$position',
        airTemperature: true,
        presentWeatherObservation: true
    }
}, {
    $unwind: '$unwindablePosition.coordinates'
}, {
    $group: {
        _id: '$_id',
        t: {$first: '$ts'},
        x: {$first: '$unwindablePosition.coordinates'},
        y: {$last: '$unwindablePosition.coordinates'},
        c: {$first: '$position.coordinates'},
        a: {$first: '$airTemperature.value'},
        w: {$first: '$presentWeatherObservation.code'}
    }
}, {
    $out: 'flattened'
}], {allowDiskUse: true});

db.flattened.ensureIndex({t: 1});
db.flattened.ensureIndex({c: '2dsphere', w: 1});
