#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse

parser = argparse.ArgumentParser(description='Convert NDN name to DNS name')

parser.add_argument('name', metavar='NAME', type=str, nargs='?',
                    help='''NDN name in URI-encoded format''')
parser.add_argument('-l', '--ltrim', dest='ltrim', action='store', type=int, default=0,
                    help='''Remove first <LTRIM> components from NDN name''')
parser.add_argument('-r', '--rtrim', dest='rtrim', action='store', type=int, default=0,
                    help='''Remove last <RTRIM> components from NDN name''')
parser.add_argument('-q', '--quiet', dest='quiet', action='store_true', default=False,
                    help='''Output minimum information (e.g., no EOL after the printed name)''')

def clean (name):
    """
    Remove ndn:/ and any leading or trailing slashes
    """
    if name.lower ().startswith ("ndn:"):
        name = name[5:]
    elif name[0] != '/':
        raise NameError ('Not a valid NDN name')

    return name.strip ("/ \t")

def dns_split (name, ltrim, rtrim):
    components = []

    # first split based on slashes
    split1 = clean (name).split ('/')
    split1 = split1[ltrim:len(split1)-rtrim]

    # second split based on periods ".", and reversing the order of components
    for component in reversed (split1):
        # print component
        split2 = component.split (".")
        components.extend (split2)

    return components

def dnsifier (ndnName, ltrim = 0, rtrim = 0):
    components = dns_split (args.name, ltrim, rtrim)
    dnsFormattedName = ".".join (components)

    # conversion to utf-8 and then idna will ensure that converted name can be DNSified
    return dnsFormattedName.decode ('utf-8').encode ('idna')

if __name__ == '__main__':
    args = parser.parse_args()
    if not args.name:
        parser.print_help ()
        exit (1)

    import sys

    try:
        dnsifiedName = dnsifier (args.name, args.ltrim, args.rtrim)
        sys.stdout.write (dnsifiedName)
        if not args.quiet:
            sys.stdout.write ("\n")

    except UnicodeError or NameError as err:
        if not args.quiet:
            sys.stderr.write ( "ERROR: %s\n" % err)
        exit (1)
