/*
 * Longest continuously-reporting stations.
 */
printjson(db.data.aggregate([{
    $sort: {
        st: 1  // station
    }
}, {
    $group: {
        _id:   '$st',  // station
        start: {$first: '$ts'},
        end:   {$last: '$ts'}
    }
}]));
