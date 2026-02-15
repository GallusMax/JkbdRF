# JkbdRF
RFID Reader acting as keyboard emulator. Written in Java and thus running on OSX, Linux and other

A visualization tool for RFID Media. Is focus is the replacement of a barcode pistol in 
libraries, plus functions regarding (un)locking media. The item Id (aka barcode) is repeated as keyboard events.
Special focus was set on keyboard-neutral implementation - see hardware list below.
 
JkbdRF is designed for quick and precise overview of tags in the field of an RFID antenna. 
This is done by recognized media showing up and dissappearing in an item list, 
according to their presence at the reader,  
including their barcode, lock state and more, if useful.
Functions can be added, such as tag locking, creation, deletion, cloning.

## Modes of operation

### Direct Mode
While in direct mode, the item Id aka "barcode" of each recognized is instantly repeated as keyboard events.
Typical use would be quite like a barcode pistol:
a) place cursor in the field, or in the first field of a list
b) bring media into antenna range
The fields fill up with the item Ids

### Triggered Mode
Triggered mode is useful, if the barcode should be called one by one.
If defined in rfid.conf, JkbdRF listens on its triggerPort for requests and will produce only keyboard events 
for one barcode at once. The (un)locking of media can also be requested this way.
This mode turns JkbdRF in a RFID module for opensource-selfcheck.
a) place media on antenna, recognized media are held in queue
b) start booking on selfcheck - start trigger 
c) media are done one by one
d) sign out stops trigger, media held in queue

### Websocket
Another mode opens a websocket and listens for special messages, answering them with JSON.


## Hardware
Tagsys readers were used during design.
While mainly bare ISO15693 inventory/readBlock/readAFI functions are used, 
an adaption to other readers should be possible with low effort.  

## Systems
JkbdRF is tested and in use on systems of different vendors 
x64/x86 Linux
Wyse T50 Ubuntu Linux
MacOSX
Windows XP/Win7/Win10/Win11
