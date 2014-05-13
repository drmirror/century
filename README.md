century
=======

This is a project to store a century's worth of weather data in MongoDB and show what you can do with it.

Loading data
============

TODO: loading instructions
--------------------------

Indexes
-------

Once the data is loaded, create the index required by the web application:

    db.data.createIndex({ts: 1});

On Jesse's laptop, building the index takes about 1 minute per million
documents. The year 1978, for example, has about 30 million documents, so
if only the year 1978 is loaded, then building the index takes about 30
minutes.

Schema description
==================

TODO.

Running the web app
===================

See `web/INSTALL.md`.