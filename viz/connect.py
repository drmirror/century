import pymongo


def get_db(port):
    # Assuming an ssh tunnel to the EC2 box over port 4321, by default:
    # ssh -L 4321:localhost:27017 century-one
    return pymongo.MongoClient(port=port).ncdc
