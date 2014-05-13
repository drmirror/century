How to install and run The Weather of the Century web application on Mac OS X.

# Install Enthought Canopy

The application needs numpy and scipy. The easiest way to get them is to
install the Enthought distribution. Download [Canopy 1.3.0][1], open the DMG,
and copy Canopy.app to your Applications folder.

[1]: https://www.enthought.com/downloads/canopy/osx-64/free/

The Canopy application isn't signed, so navigate to the Applications folder in the Finder
and right-click on it, select "open" in the menu, click through the warning.

In the "Canopy Environment setup" dialog, accept the default location and click
"Continue". When it asks, "Do you want to make Canopy your default Python environment?"
answer "No." Click "Start using Canopy." Now you can quit the Canopy application,
the rest of the setup is on the command line.

# Create a virtualenv with Canopy

Create a `venv` tool with Canopy's distribution of python:

    ~/Library/Enthought/Canopy_64bit/User/bin/venv -s ~/.virtualenvs/century

The `-s` is important to include all the packages distributed with Canopy,
like numpy. Now:

    . ~/.virtualenvs/century/bin/activate

Your prompt should now begin with `(century)`.

(Adapted and corrected from [Canopy's instructions][2].)

[2]: http://docs.enthought.com/canopy/configure/canopy-cli.html#scenario-creating-a-standalone-customizable-virtual-environment

# Clone the code

    cdvirtualenv
    git clone git@github.com:drmirror/century.git

# Install additional prereqs

Do `which pip` and ensure you're using the `pip` in your virtualenv, not the
system version. Then,

    cd century/web
    pip install -r requirements.txt

Compiling `lxml` takes some time.

# Run the app

    python century.py

Now open [http://127.0.0.1:5000/][3] in your browser. If prompted, install
the Google Earth plugin and restart your browser.

[3]: http://127.0.0.1:5000/