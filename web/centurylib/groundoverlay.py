from xml.etree import ElementTree as etree

from fastkml.kml import _Feature


class GroundOverlay(_Feature):
    """Ground overlays drape an image onto the Earth's terrain.

    Oddly, fastkml doesn't implement GroundOverlay.
    This is a sketch of an implementation, implementing only an overlay with
    an external icon.
    """

    __name__ = "GroundOverlay"
    _geometry = None
    _icon_href = None

    @property
    def geometry(self):
        return self._geometry

    @geometry.setter
    def geometry(self, geometry):
        """Takes array of [north, south, east, west, rotation]."""
        if len(geometry) != 5:
            raise ValueError

        self._geometry = geometry

    @property
    def icon_href(self):
        return self._icon_href

    @icon_href.setter
    def icon_href(self, icon_href):
        self._icon_href = icon_href

    def etree_element(self):
        element = super(GroundOverlay, self).etree_element()

        if self._icon_href is not None:
            icon = etree.SubElement(element, 'Icon')
            href = etree.SubElement(icon, 'href')
            href.text = self._icon_href

        if self._geometry is not None:
            latlonbox = etree.SubElement(element, 'LatLonBox')
            for name, value in zip(
                    ['north', 'south', 'east', 'west', 'rotation'],
                    self._geometry):
                el = etree.SubElement(latlonbox, name)
                el.text = str(value)

        return element
