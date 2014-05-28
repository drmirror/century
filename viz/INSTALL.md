# Mac installation instructions

## Geos and Basemap

We want matplotlib's "basemap" extension but "pip install basemap" failed
for me, with "Can't find geos library." Install from source instead. [Download
basemap 1.0.7][1] and:

    tar xzvf basemap-1.0.7.tar.gz
    cd basemap-1.0.7
    cd geos-3.3.3
    ./configure; make; sudo make install

Annoyingly, Geos is installed in /include and /lib, which isn't a usual place
on Mac OS, so you'll need to use CFLAGS and LDFLAGS to tell the compiler where
to find Geos in the next step.

First, activate your virtualenv so you're using the Enthought Canopy Python.
Then:

    cd ..
    export ARCHFLAGS='-arch x86_64'
    LDFLAGS="-L/lib" CFLAGS="-I/include" GEOS_DIR=/lib python setup.py install

If you get this:

    clang: warning: no such sysroot directory: '/Developer/SDKs/MacOSX10.6.sdk'

Then download Command Line Tools for XCode[2] and run the installer.

    sudo mkdir -p /Developer/SDKs/
    sudo ln -s /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX10.9.sdk /Developer/SDKs/MacOSX10.6.sdk

And try again:

    LDFLAGS="-L/lib" CFLAGS="-I/include" LDFLAGS GEOS_DIR=/lib python setup.py install

Success looks like this at the end:

    Writing /Users/emptysquare/.virtualenvs/century/lib/python2.7/site-packages/basemap-1.0.7-py2.7.egg-info

[1]: http://sourceforge.net/projects/matplotlib/files/matplotlib-toolkits/basemap-1.0.7/

[2]: http://adcdownload.apple.com/Developer_Tools/command_line_tools_os_x_mavericks_for_xcode__april_2014/command_line_tools_for_osx_mavericks_april_2014.dmg

## Other Python dependencies

Finally we can install our Python packages:

    cdvirtualenv century/web/experiments
    pip install -r requirements.txt

You'll see a lot of "Using deprecated NumPy API" warnings while installing
netCDF4.

[7]: https://pypi.python.org/pypi/netCDF4/

## ffmpeg

Download from http://ffmpegmac.net/
