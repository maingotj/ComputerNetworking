# ComputerNetworking

## Group 53

## Members

Jonathan Maingot    maingotj@ufl.edu    74425040

Andres Martinez     amartinez6@ufl.edu  92809160

Bradley Meyer       bmeyer1@ufl.edu     50671569

## Contributions

Jonathan Maingot: Protocol Functions and Peer Connection

Andres Martinez: Log and Protocol Functions

Bradley Meyer: Peer Connection

## Youtube Link

https://youtu.be/uX78EVoorGQ

### Start the peer processes: 35%

In this section, we successfully implemented the initialization and start of the peer processes. We demonstrated the program's ability to read configuration files, specifically `Common.cfg` and `PeerInfo.cfg`, to correctly set related variables. Additionally, the program allows each peer to establish TCP connections to all peers that started before it. The initiation of the exchange of pieces, as described in the protocol, occurs when a peer is connected to at least one other peer. The termination condition is also implemented, ensuring that a peer terminates when it determines that all peers, not just itself, have downloaded the complete file.

### After connection: 30%

After establishing a connection, the program handles various aspects of the protocol. It correctly implements the handshake message, ensuring that each peer sends a handshake message to the other before engaging in other message exchanges. The exchange of bitfield messages occurs after the handshake to indicate the pieces the peer has. The program also sends 'interested' or 'not interested' messages based on the availability of the peer's pieces. It periodically sends 'unchoke' and 'choke' messages every p seconds and sets an optimistically unchoked neighbor every 'm' seconds.

### File exchange: 30%

In the file exchange section, the program successfully sends 'request' messages to request specific pieces from peers. It also sends 'have' messages to inform other peers about having certain pieces. 'Not interested' and 'interested' messages are sent as needed. The program is designed to send 'piece' messages, delivering the content of a piece to a peer. It correctly handles received 'have' messages, updating the related bitfield accordingly. While there are some issues preventing the correct termination of the service and writing to the file, the majority of the code for these functionalities is present.

### Stopping the service correctly: 5%

Although there are challenges in stopping the service correctly, and there are bugs impacting the overall functionality, the implementation covers a significant portion of the required features. Further refinement is needed to resolve issues and improve the overall reliability of the program.

## How to compile

Make sure to have a sample Common.cfg and PeerInfo.cfg for the program to read

use the command java peerProcess peerID

switch the peerID with a peerID present in the PeerInfo.cfg

# Project Overview

We designed a P2P file sharing software using the Java programming language. 

## Protocols

Handshake: Happens at initiala connection to begin file sharing.

Choke and unchoke: This indicated to other peers whether or not they are open to being sent pieces.

Interested and not interested: This allows the peer to express whether another peer has pieces it needs.

Have: This tells other peers if it has the piece(s) that they want.

Bitfield: Sent after the handshake to show what pieces the peer has.

Request: This sends a request for a certain piece from a peer.

Piece: This sends the content of a piece to a peer
