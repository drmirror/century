import os

from lxml import etree


out_xml_root = '''
<?xml version="1.0" encoding="utf-8" ?>
<kml xmlns="http://www.opengis.net/kml/2.2" />
'''.strip()


def write_state(placemark, state_name, out_dir):
    out_file_name = '%s.kml' % state_name.lower()
    out_path = os.path.join(out_dir, out_file_name)
    namespaces = {'kml': 'http://www.opengis.net/kml/2.2'}

    out_root = etree.XML(out_xml_root)
    out_placemark = etree.SubElement(out_root, 'Placemark')
    etree.SubElement(out_placemark, 'name').text = state_name
    out_placemark.append(
        etree.XML('''
            <Style>
                <LineStyle><color>ff0000ff</color></LineStyle>
                <PolyStyle><fill>0</fill></PolyStyle>
            </Style>'''))

    geometry = (
        placemark.find('kml:MultiGeometry', namespaces=namespaces)
        or placemark.find('kml:Polygon', namespaces=namespaces))

    if geometry is None:
        raise AssertionError(
            "No MultiGeometry or Polygon found for %s" % state_name)

    out_placemark.append(geometry)
    with open(out_path, 'w+') as out:
        out.write(etree.tostring(out_root, pretty_print=True))


def main():
    this_dir = os.path.dirname(__file__)
    filename = os.path.join(this_dir, 'gz_2010_us_040_00_500k.kml')
    out_dir = os.path.normpath(
        os.path.join(this_dir, '../web/static/states-kml'))

    print('loading %s' % filename)
    root = etree.parse(filename)
    namespaces = {'kml': 'http://www.opengis.net/kml/2.2'}
    folder = root.find('kml:Document/kml:Folder', namespaces=namespaces)
    for p in folder.findall('kml:Placemark', namespaces=namespaces):
        state_name = p.find('kml:name', namespaces=namespaces).text
        print state_name
        write_state(p, state_name, out_dir)


if __name__ == '__main__':
    main()
