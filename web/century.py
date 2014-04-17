from flask import Flask, render_template, request, abort
from centurylib import data, parsing

app = Flask(__name__)


@app.route("/")
def index():
    return render_template('index.html')


@app.route("/samples.kml")
def samples():
    date_str = request.args['date']
    dt = parsing.parse_date_str(date_str)
    if not dt:
        abort(400, "Can't parse date %r" % date_str)

    # Placemarks.
    rv = data.stations(dt, prettyprint=app.debug)
    print rv
    return rv


if __name__ == "__main__":
    app.run(debug=True)
