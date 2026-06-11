package com.bb3.bb3chat.feature.pairing.domain.model

data class RoomCodeMatch(
    val roomId   : String,
    val myCode   : String,
    val peerCode : String
)
