/**
 * @file ndnr_io.h
 * 
 * Part of ndnr - NDNx Repository Daemon.
 *
 */

/*
 * Portions Copyright (C) 2013 Regents of the University of California.
 * 
 * Based on the CCNx C Library by PARC.
 * Copyright (C) 2011 Palo Alto Research Center, Inc.
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
 
#ifndef NDNR_IO_DEFINED
#define NDNR_IO_DEFINED

#include "ndnr_private.h"

void r_io_shutdown_all(struct ndnr_handle *h);
void r_io_prepare_poll_fds(struct ndnr_handle *h);
void r_dispatch_process_internal_client_buffer(struct ndnr_handle *h);
void r_io_send(struct ndnr_handle *h,struct fdholder *fdholder,const void *data,size_t size,off_t *offsetp);
int r_io_destroy_face(struct ndnr_handle *h,unsigned filedesc);
int r_io_open_repo_data_file(struct ndnr_handle *h, const char *name, int output);
int r_io_repo_data_file_fd(struct ndnr_handle *h, unsigned repofile, int output);
void r_io_shutdown_client_fd(struct ndnr_handle *h,int fd);
int r_io_accept_connection(struct ndnr_handle *h,int listener_fd);
struct fdholder *r_io_record_fd(struct ndnr_handle *h,int fd,void *who,socklen_t wholen,int setflags);
void r_io_register_new_face(struct ndnr_handle *h,struct fdholder *fdholder);
int r_io_enroll_face(struct ndnr_handle *h,struct fdholder *fdholder);
struct fdholder *ndnr_r_io_fdholder_from_fd(struct ndnr_handle *h,unsigned filedesc);
struct fdholder *r_io_fdholder_from_fd(struct ndnr_handle *h,unsigned filedesc);

#endif
