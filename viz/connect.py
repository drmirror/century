import pymongo


def get_db():
    # Assuming an ssh tunnel to the EC2 box over port 4321:
    # ssh -L 4321:localhost:27017 century-one
    return pymongo.MongoClient(port=4321).ncdc
