How to install and run The Weather of the Century web application on Mac OS X.

# Install Enthought Canopy

The application needs numpy and scipy. The easiest way to get them is to
install the Enthought distribution. Download [Canopy 1.3.0][1], open the DMG,
and copy Canopy.app to your Applications folder.

[1]: https://www.enthought.com/downloads/canopy/osx-64/free/

# Create a virtualenv with Canopy

Create a `venv` tool with Canopy's distribution of python, then use it to
create a virtualenv, then activate it:

    /Applications/Canopy.app/Contents/MacOS/Canopy_cli setup ~/canopy_base_env
    ~/canopy_base_env/bin/venv -s ~/.virtualenvs/century
    . ~/.virtualenvs/century/bin/activate

Your prompt should now begin with `(century)`.

(Based on [Canopy's instructions][2].)

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
