import re
import datetime

# "yyyy-mm-dd"
date_pat = re.compile(r'(\d{4})-(\d{2})-(\d{2})')


def parse_date_str(date_str):
    date_match = date_pat.match(date_str)
    if not date_match:
        return None

    year, month, day = date_match.groups()
    return datetime.datetime(int(year), int(month), int(day))
