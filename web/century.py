from flask import Flask, render_template, request, abort
import re

app = Flask(__name__)


@app.route("/")
def index():
    return render_template('index.html')


# "yyyy-mm-dd"
date_pat = re.compile(r'(\d{4})-(\d{2})-(\d{2})')


@app.route("/samples.kml")
def samples():
    date_str = request.args['date']
    date_match = date_pat.match(date_str)
    if not date_match:
        abort(400, "Can't parse date %r" % date_str)

    return open('kml2.kml').read()

if __name__ == "__main__":
    app.run(debug=True)
