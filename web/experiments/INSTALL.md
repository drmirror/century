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

## SZIP, HDF5, and NetCDF

This section is obsolete, I just wanted to see what NetCDF could do. Skip
over the SZIP, HDF5, and NetCDF installation and go straight
to [Other Python dependencies](#other-python-dependencies).

***

Based on these instructions[3] with some updates.

[3]: http://cdx.jpl.nasa.gov/documents/technical-design/accessing-hdf-data-from-python-on-mac-os-x

### libjpeg and libpng

Download [libjpeg and libpng][4] and run the installer.

[4]: http://ethan.tira-thompson.com/Mac%20OS%20X%20Ports_files/libjpeg-libpng%20%28universal%29.dmg

### SZIP

Make sure you're still in your virtualenv. `$VIRTUAL_ENV` should be set in
your environment. Install SZIP:

    curl ftp://ftp.hdfgroup.org/lib-external/szip/2.1/src/szip-2.1.tar.gz | tar xzf -
    cd szip-2.1
    ./configure --prefix=$VIRTUAL_ENV --enable-shared --enable-production
    make
    make check

If success, you'll see "All test passed." Now:

    make install
    cd ..

Should see:

    Libraries have been installed in:
       /Users/emptysquare/.virtualenvs/century/lib

### HDF5

Install HDF5 from [these instructions][5] with some corrections:

    curl http://www.hdfgroup.org/ftp/HDF5/current/src/hdf5-1.8.12.tar.gz | tar xzf -
    cd hdf5-1.8.12
    ./configure \
        --prefix=$VIRTUAL_ENV \
        --enable-shared \
        --enable-production \
        --with-szlib=$VIRTUAL_ENV \
        CPPFLAGS=-I$VIRTUAL_ENV/include \
        LDFLAGS=-L$VIRTUAL_ENV/lib
    make install

(We configured without `--enable-threadsafe` because we don't think we
need it, and it avoids an error:
"implicit declaration of function 'pthread_once' is invalid in C99".)

[5]: http://cdx.jpl.nasa.gov/documents/technical-design/accessing-hdf-data-from-python-on-mac-os-x

### NETCDF

    curl ftp://ftp.unidata.ucar.edu/pub/netcdf/netcdf.tar.gz | tar xvzf -
    cd netcdf-4.3.0
    ./configure \
        --prefix=$VIRTUAL_ENV \
        --with-szlib=$VIRTUAL_ENV \
        --enable-netcdf-4 \
        CPPFLAGS=-I$VIRTUAL_ENV/include \
        LDFLAGS=-L$VIRTUAL_ENV/lib

(We're surprised to see a warning: "unrecognized options: --with-szlib".)

Don't run `make` immediately. Based on [this discussion][6] you now need to
insert a line at the beginning of `genlib.h`:

    echo -e "#include <config.h>\n$(cat ncgen3/genlib.h)" > ncgen3/genlib.h

Now:

    make
    make install

[6]: https://github.com/Homebrew/homebrew-science/issues/369#issuecomment-27216871

The library has been installed into your virtualenv's lib directory and
you'll see a big "Congratulations!" message.

## Other Python dependencies<a id="other-py-deps"></a>

Finally we can install our Python packages. The prior work has focused on
making the [netCDF4][7] package installable:

    export HDF5_DIR=$VIRTUAL_ENV
    export NETCDF4_DIR=$VIRTUAL_ENV
    cdvirtualenv century/web/experiments
    pip install -r requirements.txt

You'll see a lot of "Using deprecated NumPy API" warnings while installing
netCDF4.

[7]: https://pypi.python.org/pypi/netCDF4/
