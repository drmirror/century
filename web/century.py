from flask import Flask, render_template

app = Flask(__name__)


@app.route("/")
def index():
    return render_template('index.html')


@app.route("/kml.kml")
def kml():
    return open('kml2.kml').read()

if __name__ == "__main__":
    app.run(debug=True)
