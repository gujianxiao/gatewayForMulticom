/**
 * @file signbenchtest.c
 * 
 * A simple test program to benchmark signing performance.
 *
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2009 Palo Alto Research Center, Inc.
 *
 * This work is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 * This work is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details. You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */
 #include <stdlib.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include <ndn/ndn.h>
#include <ndn/charbuf.h>
#include <ndn/keystore.h>
#include <time.h>
#include <sys/time.h>

#define FRESHNESS 10 
#define COUNT 3000
#define PAYLOAD_SIZE 51

int
main(int argc, char **argv)
{

  struct ndn_keystore *keystore = NULL;
  int res = 0;
  struct ndn_charbuf *signed_info = ndn_charbuf_create();
  int i;
  int sec, usec;
  char msgbuf[PAYLOAD_SIZE];
  struct timeval start, end;
  struct ndn_charbuf *message = ndn_charbuf_create();
  struct ndn_charbuf *path = ndn_charbuf_create();
  struct ndn_charbuf *seq = ndn_charbuf_create();

  struct ndn_charbuf *temp = ndn_charbuf_create();
  keystore = ndn_keystore_create();
  ndn_charbuf_putf(temp, "%s/.ndnx/.ndnx_keystore", getenv("HOME"));
  res = ndn_keystore_init(keystore,
			  ndn_charbuf_as_string(temp),
			  "Th1s1sn0t8g00dp8ssw0rd.");
  if (res != 0) {
    printf("Failed to initialize keystore %s\n", ndn_charbuf_as_string(temp));
    exit(1);
  }
  ndn_charbuf_destroy(&temp);
  
  res = ndn_signed_info_create(signed_info,
			       /* pubkeyid */ ndn_keystore_public_key_digest(keystore),
			       /* publisher_key_id_size */ ndn_keystore_public_key_digest_length(keystore),
			       /* datetime */ NULL,
			       /* type */ NDN_CONTENT_DATA,
			       /* freshness */ FRESHNESS,
                               /*finalblockid*/ NULL,
			       /* keylocator */ NULL);

  srandom(time(NULL));
  for (i=0; i<PAYLOAD_SIZE; i++) {
    msgbuf[i] = random();
  }

  printf("Generating %d signed ContentObjects (one . per 100)\n", COUNT);
  gettimeofday(&start, NULL);

  for (i=0; i<COUNT; i++) {
    
    if (i>0 && (i%100) == 0) {
      printf(".");
      fflush(stdout);
    }
    ndn_name_init(path);
    ndn_name_append_str(path, "rtp");
    ndn_name_append_str(path, "protocol");
    ndn_name_append_str(path, "13.2.117.34");
    ndn_name_append_str(path, "domain");
    ndn_name_append_str(path, "smetters");
    ndn_name_append_str(path, "principal");
    ndn_name_append_str(path, "2021915340");
    ndn_name_append_str(path, "id");
    ndn_charbuf_putf(seq, "%u", i);
    ndn_name_append(path, seq->buf, seq->length);
    ndn_name_append_str(path, "seq");
  
    res = ndn_encode_ContentObject(/* out */ message,
				   path, signed_info, 
				   msgbuf, PAYLOAD_SIZE,
				   ndn_keystore_digest_algorithm(keystore), 
				   ndn_keystore_private_key(keystore));

    ndn_charbuf_reset(message);
    ndn_charbuf_reset(path);
    ndn_charbuf_reset(seq);
  }
  gettimeofday(&end, NULL);
  sec = end.tv_sec - start.tv_sec;
  usec = (int)end.tv_usec - (int)start.tv_usec;
  while (usec < 0) {
    sec--;
    usec += 1000000;
  }

  printf("\nComplete in %d.%06d secs\n", sec, usec);

  return(0);
}
