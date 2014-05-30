# Mac installation instructions

## Install Enthought Canopy

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

## Create a virtualenv with Canopy

Create a `venv` tool with Canopy's distribution of python:

    ~/Library/Enthought/Canopy_64bit/User/bin/venv -s ~/.virtualenvs/century

The `-s` is important to include all the packages distributed with Canopy,
like numpy. Now:

    . ~/.virtualenvs/century/bin/activate

Your prompt should now begin with `(century)`.

(Adapted and corrected from [Canopy's instructions][2].)

[2]: http://docs.enthought.com/canopy/configure/canopy-cli.html#scenario-creating-a-standalone-customizable-virtual-environment

## Clone the code

    cdvirtualenv
    git clone git@github.com:drmirror/century.git

## Install additional prereqs

Do `which pip` and ensure you're using the `pip` in your virtualenv, not the
system version. Then,

    cd viz
    pip install -r requirements.txt

## Geos and Basemap

We want matplotlib's "basemap" extension but "pip install basemap" failed
for me, with "Can't find geos library." Install from source instead. Change
to a temporary directory, [download basemap 1.0.7][3] and:

    tar xzvf basemap-1.0.7.tar.gz
    cd basemap-1.0.7
    cd geos-3.3.3
    ./configure; make; sudo make install

Annoyingly, Geos is installed in /include and /lib, which isn't a usual place
on Mac OS, so you'll need to use CFLAGS and LDFLAGS to tell the compiler where
to find Geos in the next step.

Make sure your Enthought Canopy Python virtualenv is still active.
Then:

    cd ..
    export ARCHFLAGS='-arch x86_64'
    LDFLAGS="-L/lib" CFLAGS="-I/include" GEOS_DIR=/lib python setup.py install

If you get this:

    clang: warning: no such sysroot directory: '/Developer/SDKs/MacOSX10.6.sdk'

Then download Command Line Tools for XCode[4] and run the installer.

    sudo mkdir -p /Developer/SDKs/
    sudo ln -s /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX10.9.sdk /Developer/SDKs/MacOSX10.6.sdk

And try again:

    LDFLAGS="-L/lib" CFLAGS="-I/include" LDFLAGS GEOS_DIR=/lib python setup.py install

Success looks like this at the end:

    Writing /Users/emptysquare/.virtualenvs/century/lib/python2.7/site-packages/basemap-1.0.7-py2.7.egg-info

[3]: http://sourceforge.net/projects/matplotlib/files/matplotlib-toolkits/basemap-1.0.7/

[4]: http://adcdownload.apple.com/Developer_Tools/command_line_tools_os_x_mavericks_for_xcode__april_2014/command_line_tools_for_osx_mavericks_april_2014.dmg

## Other Python dependencies

Finally we can install our Python packages:

    cdvirtualenv century
    pip install -r viz/requirements.txt

## ffmpeg

Required for generating movies with plotweather.py.

Download from http://ffmpegmac.net/

## Run plotweather.py

For instructions:

    cdvirtualenv century
    python -m vz.plotweather --help
