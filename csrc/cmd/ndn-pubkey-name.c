/**
 * @file ndn-pubkey-name.c
 *
 * @brief Command line utility to print out public key name stored in .pubcert file
 *
 * Copyright (c) 2013 University of California, Los Angeles
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2.1 as
 * published by the Free Software Foundation;
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

#include <ndn/ndn.h>
#include <ndn/uri.h>
#include <stdio.h>

int
main(int argc, char *argv[])
{
  struct ndn *ndn = NULL;
  int res = 0;

  ndn = ndn_create ();
  if (ndn != NULL) {
    struct ndn_charbuf *name;
    struct ndn_signing_params sp = NDN_SIGNING_PARAMS_INIT;

    name = ndn_charbuf_create ();
    res = ndn_get_public_key_and_name (ndn, &sp, NULL, NULL, name);

    if (res >= 0 && name->length > 0) {
      struct ndn_charbuf *uri_name = ndn_charbuf_create ();

      ndn_uri_append (uri_name, name->buf, name->length, 0);
      fprintf (stdout, "%s\n", ndn_charbuf_as_string (uri_name));

      res = 0;
      ndn_charbuf_destroy (&uri_name);
    }
    else {
      fprintf (stderr, "ERROR: cannot load public key or public key name\n");
      res = -2;
    }

    ndn_charbuf_destroy (&name);
  }
  else
    {
      fprintf (stderr, "ERROR: cannot create ndn handle\n");
      res = -1;
    }

  return res;
}
