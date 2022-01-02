package ws.furrify.artists.avatar;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import ws.furrify.artists.avatar.dto.AvatarDTO;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class AvatarTest implements CommandLineRunner {

    private final SqlAvatarRepository sqlAvatarRepository;
    private final Environment environment;

    @Override
    public void run(final String... args) {
        Arrays.stream(environment.getActiveProfiles())
                .filter("dev"::equals)
                .findAny()
                .ifPresent((profile) -> createTestingArtists());
    }

    @SneakyThrows
    private void createTestingArtists() {
        var avatarFactory = new AvatarFactory();

        var userId = UUID.fromString("f4612c64-f11e-4d73-8f2f-006303287f35");
        var artistId = UUID.fromString("9551e7e0-4550-41b9-8c4a-57943642fa00");
        var avatarId = UUID.fromString("4d482df8-7380-4164-96ef-58f3796d8f27");

        sqlAvatarRepository.save(
                avatarFactory.from(
                        AvatarDTO.builder()
                                .avatarId(avatarId)
                                .artistId(artistId)
                                .ownerId(userId)
                                .extension(AvatarExtension.PNG)
                                .filename("test.png")
                                .fileUri(new URI("/test"))
                                .thumbnailUri(new URI("/test"))
                                .md5("3c518eeb674c71b30297f072fde7eba5")
                                .createDate(ZonedDateTime.now())
                                .build()
                ).getSnapshot()
        );

        System.out.println("AvatarId: " + artistId);
    }

}
