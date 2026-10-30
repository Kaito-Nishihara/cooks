package app.recipebook.auth.token.infrastructure;

import app.recipebook.auth.config.AuthSettings;
import app.recipebook.auth.token.application.PublicKeySet;
import app.recipebook.auth.token.application.PublicKeyView;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jboss.logging.Logger;

/**
 * 署名の鍵を、ファイルから読んで持つ(「共通設計: 認証」の 4.8)。
 * 読めないときは、起動を止めずに、要求を受けられない状態(稼働の確認の ready が否)にする。
 * 次に使うときに、もう一度読む。Secret が後から載った場合に、起動し直さずに直るようにするためである。
 */
@ApplicationScoped
public class SigningKeys implements PublicKeySet {

    private static final Logger LOG = Logger.getLogger(SigningKeys.class);

    private final String signingKeyFile;
    private final List<String> retiredPublicKeyFiles;
    private volatile Loaded loaded;

    /** 読み終えた鍵。署名の鍵と、公開の鍵の一覧 */
    record Loaded(String keyId, RSAPrivateCrtKey privateKey, List<PublicKeyView> published) {
    }

    @Inject
    public SigningKeys(AuthSettings settings) {
        this(settings.signingKeyFile(), settings.retiredPublicKeyFiles().orElse(List.of()));
    }

    SigningKeys(String signingKeyFile, List<String> retiredPublicKeyFiles) {
        this.signingKeyFile = signingKeyFile;
        this.retiredPublicKeyFiles = List.copyOf(retiredPublicKeyFiles);
    }

    void loadOnStart(@Observes StartupEvent event) {
        current();
    }

    /** 署名に使う鍵を返す。読めないときは空を返す。 */
    public Optional<Loaded> current() {
        Loaded value = loaded;
        if (value == null) {
            value = load();
            loaded = value;
        }
        return Optional.ofNullable(value);
    }

    /** 署名の鍵を読めているかどうかを返す。 */
    public boolean isReady() {
        return current().isPresent();
    }

    @Override
    public List<PublicKeyView> publish() {
        return current().map(Loaded::published).orElse(List.of());
    }

    private Loaded load() {
        try {
            RSAPrivateCrtKey privateKey = RsaKeys.readPrivateKey(Path.of(signingKeyFile));
            RSAPublicKey publicKey = RsaKeys.publicKeyOf(privateKey);
            List<PublicKeyView> published = new ArrayList<>();
            published.add(viewOf(publicKey));
            for (String file : retiredPublicKeyFiles) {
                published.add(viewOf(RsaKeys.readPublicKey(Path.of(file))));
            }
            String keyId = published.get(0).keyId();
            LOG.infof("署名の鍵を読みました。鍵の名前: %s、公開する鍵の数: %d", keyId, published.size());
            return new Loaded(keyId, privateKey, List.copyOf(published));
        } catch (IOException | GeneralSecurityException | RuntimeException e) {
            // 鍵の中身は出さない。どのファイルで、何が起きたかだけを出す
            LOG.errorf("署名の鍵を読めません。設定 app.auth.signing-key-file と、鍵のファイルを確かめてください。理由: %s",
                    e.getMessage());
            return null;
        }
    }

    private static PublicKeyView viewOf(RSAPublicKey key) {
        return new PublicKeyView(RsaKeys.keyIdOf(key), RsaKeys.base64Url(key.getModulus()),
                RsaKeys.base64Url(key.getPublicExponent()));
    }
}
