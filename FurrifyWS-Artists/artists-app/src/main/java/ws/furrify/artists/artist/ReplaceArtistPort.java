package ws.furrify.artists.artist;

import ws.furrify.artists.artist.dto.ArtistDTO;

import java.util.UUID;

interface ReplaceArtistPort {
    void replaceArtist(UUID ownerId, UUID artistId, ArtistDTO artistDTO);
}
