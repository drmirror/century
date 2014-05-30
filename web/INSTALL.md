How to install and run The Weather of the Century web application on Mac OS X.

# Clone the code

    git clone git@github.com:drmirror/century.git

# Install additional prereqs

    cd century/web
    pip install -r requirements.txt

Compiling `lxml` takes some time.

# Run the app

    python century.py

Now open [http://127.0.0.1:8000/][1] in your browser. If prompted, install
the Google Earth plugin and restart your browser.

[1]: http://127.0.0.1:8000/
