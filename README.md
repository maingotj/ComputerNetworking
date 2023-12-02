# ComputerNetworking

## Group 53

## Members

Jonathan Maingot    maingotj@ufl.edu    74425040

Andres Martinez

Bradley Meyer

## Contributions

## Youtube Link

## What we accomplished

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
