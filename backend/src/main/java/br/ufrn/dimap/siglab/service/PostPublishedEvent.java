package br.ufrn.dimap.siglab.service;

/**
 * Evento de domínio publicado quando um post é criado em um grupo de pesquisa.
 * <p>
 * Carrega um retrato do post publicado para que a notificação aos seguidores
 * não dependa de nova leitura do banco nem da disponibilidade da transação que
 * originou o evento.
 * </p>
 *
 * @param groupId         identificador do grupo onde o post foi publicado
 * @param postId          identificador do post publicado
 * @param publisherUserId identificador do usuário que publicou o post
 * @param title           título do post
 * @param description     descrição do post, quando informada
 * @param content         conteúdo do post
 * @param tags            etiquetas do post
 * @param keywords        palavras-chave do post
 */
public record PostPublishedEvent(String groupId,
                                  Long postId,
                                  Long publisherUserId,
                                  String title,
                                  String description,
                                  String content,
                                  String[] tags,
                                  String[] keywords) {
}
