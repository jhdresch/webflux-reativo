package com.br.webfluxreativo.service;

import com.br.webfluxreativo.entity.UserEntity;
import com.br.webfluxreativo.mapper.UserMapper;
import com.br.webfluxreativo.model.dtos.UserRequest;
import com.br.webfluxreativo.model.dtos.UserResponse;
import com.br.webfluxreativo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    // Mocks: simulam o comportamento do repositório e do mapper sem realizar operações reais
    @Mock
    private UserRepository repository;

    @Mock
    private UserMapper mapper;

    // Classe sob teste: será injetada com os mocks acima
    @InjectMocks
    private UserService service;

    // --- helpers de teste ---
    // Cria um objeto UserRequest de exemplo (entrada de criação/atualização)
    private UserRequest sampleRequest() {
        return new UserRequest("John", "john@example.com", "secret123");
    }

    // Cria uma entidade de usuário de exemplo (simula o que está no banco)
    private UserEntity sampleEntity(String id) {
        return UserEntity.builder()
                .id(id)
                .name("John")
                .email("john@example.com")
                .password("secret123")
                .build();
    }

    // Cria um DTO de resposta mapeado
    private UserResponse sampleResponse(String id) {
        return new UserResponse(id, "John", "john@example.com");
    }

    // --- testes ---
    @Test
    void listarTodos_deveRetornarRespostasMapeadas() {
        // Arrange: preparar duas entidades e configurar os mocks
        UserEntity e1 = sampleEntity("1");
        UserEntity e2 = sampleEntity("2");

        // repository.findAll() retorna um Flux com e1 e e2
        when(repository.findAll()).thenReturn(Flux.just(e1, e2));
        // mapper converte cada entidade em UserResponse correspondente
        when(mapper.toResponse(e1)).thenReturn(sampleResponse("1"));
        when(mapper.toResponse(e2)).thenReturn(sampleResponse("2"));

        // Act + Assert: usar StepVerifier para inspecionar o Flux retornado pelo service
        StepVerifier.create(service.findAll())
                .expectNextMatches(r -> r.id().equals("1"))
                .expectNextMatches(r -> r.id().equals("2"))
                .verifyComplete();

        // Verifica que o repositório foi chamado
        verify(repository).findAll();
    }

    @Test
    void buscarPorId_quandoEncontrado_deveRetornarResposta() {
        // Arrange: entidade encontrada
        UserEntity e = sampleEntity("1");
        when(repository.findById("1")).thenReturn(Mono.just(e));
        when(mapper.toResponse(e)).thenReturn(sampleResponse("1"));

        // Act + Assert: o service deve retornar um Mono com o UserResponse correspondente
        StepVerifier.create(service.findById("1"))
                .expectNextMatches(r -> r.id().equals("1") && r.email().equals("john@example.com"))
                .verifyComplete();
    }

    @Test
    void buscarPorId_quandoNaoEncontrado_deveCompletarVazio() {
        // Arrange: repositório retorna vazio
        when(repository.findById("missing")).thenReturn(Mono.empty());

        // Act + Assert: o Mono deve completar sem elementos
        StepVerifier.create(service.findById("missing"))
                .verifyComplete();
    }

    @Test
    void criar_deveSalvarERetornarResposta() {
        // Arrange: converter request -> entidade, salvar e mapear para response
        UserRequest req = sampleRequest();
        UserEntity toSave = sampleEntity(null);
        UserEntity saved = sampleEntity("generated-id");

        when(mapper.toEntity(req)).thenReturn(toSave);
        when(repository.save(toSave)).thenReturn(Mono.just(saved));
        when(mapper.toResponse(saved)).thenReturn(sampleResponse("generated-id"));

        // Act + Assert: service.create deve emitir o UserResponse com id gerado
        StepVerifier.create(service.create(req))
                .expectNextMatches(r -> r.id().equals("generated-id"))
                .verifyComplete();

        // Verifica que repository.save foi invocado com a entidade convertida
        verify(repository).save(toSave);
    }

    @Test
    void atualizar_quandoEncontrado_deveSalvarERetornarResposta() {
        // Arrange: entidade existente no repositório
        String id = "1";
        UserRequest req = sampleRequest();
        UserEntity existing = sampleEntity(id);
        UserEntity saved = sampleEntity(id);

        when(repository.findById(id)).thenReturn(Mono.just(existing));
        // mapper.atualizarDeRequest é void; aqui simulamos que será invocado sem lançar exceção
        doAnswer(invocation -> { return null; }).when(mapper).updateFromRequest(eq(req), eq(existing));
        when(repository.save(existing)).thenReturn(Mono.just(saved));
        when(mapper.toResponse(saved)).thenReturn(sampleResponse(id));

        // Act + Assert: service.atualizar deve retornar o UserResponse após salvar
        StepVerifier.create(service.update(id, req))
                .expectNextMatches(r -> r.id().equals(id))
                .verifyComplete();
        // Verificações: mapper.atualizarDeRequest e repository.save foram chamados
        verify(mapper).updateFromRequest(eq(req), eq(existing));
        verify(repository).save(existing);
    }

    @Test
    void atualizar_quandoNaoEncontrado_deveCompletarVazio() {
        // Arrange: entidade não existe
        String id = "missing";
        UserRequest req = sampleRequest();
        when(repository.findById(id)).thenReturn(Mono.empty());

        // Act + Assert: update completa vazio quando não há entidade para atualizar
        StepVerifier.create(service.update(id, req))
                .verifyComplete();
    }

    @Test
    void deletar_quandoEncontrado_deveRetornarTrue() {
        // Arrange: entidade existe e será deletada
        String id = "1";
        UserEntity existing = sampleEntity(id);

        when(repository.findById(id)).thenReturn(Mono.just(existing));
        when(repository.delete(existing)).thenReturn(Mono.empty());

        // Act + Assert: service.deletar deve emitir true quando excluir com sucesso
        StepVerifier.create(service.delete(id))
                .expectNext(true)
                .verifyComplete();
        verify(repository).delete(existing);
    }

    @Test
    void deletar_quandoNaoEncontrado_deveRetornarFalse() {
        // Arrange: entidade não existe
        String id = "missing";
        when(repository.findById(id)).thenReturn(Mono.empty());

        // Act + Assert: service.delete deve emitir false quando não encontra a entidade
        StepVerifier.create(service.delete(id))
                .expectNext(false)
                .verifyComplete();
    }
}

