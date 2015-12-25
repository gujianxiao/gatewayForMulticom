In order to build the NDN plugin for Wireshark you will first need to install
a Wireshark source distribution.

Then -- in your wireshark source directory you should make a directory
wireshark/plugins/ndn and copy the contents of the NDN distribution's
ndn/apps/wireshark/ndn there.

You'll need to follow the instructions in wireshark file doc/README.plugins
regarding existing Wireshark files that need to be edited to add a new plugin.

For wireshark 1.0.7, you need to fix the following files (README.plugins sec 3)

	configure.in
	epan/Makefile.am
	Makefile.am
	Makefile.nmake
	packaging/nsis/Makefile.nmake
	packaging/nsis/wireshark.nsi
	plugins/Makefile.am
	plugins/Makefile.nmake

When you've made the changes per README.plugins, you must run wireshark's
autogen.sh and configure to setup your wireshark build area, and then
recompile wireshark.

If you wish to use the ndn plugin with a version of Wireshark earlier than
1.4.x, see the note in packet-ndn.c regarding the use of BASE_NONE instead
of ABSOLUTE_TIME_LOCAL in the definition of the ndn.timestamp field.
