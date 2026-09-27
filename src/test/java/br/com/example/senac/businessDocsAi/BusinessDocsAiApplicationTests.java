package br.com.example.senac.businessDocsAi;

import br.com.example.senac.businessDocsAi.categories.controller.CategoryController;
import br.com.example.senac.businessDocsAi.categories.dto.CategoryRequestDTO;
import br.com.example.senac.businessDocsAi.categories.dto.CategoryResponseDTO;
import br.com.example.senac.businessDocsAi.categories.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessDocsAiApplicationTests {

	@Mock
	private CategoryService service;

	@InjectMocks
	private CategoryController controller;

	@Test
	void shouldListCategories() {
		CategoryResponseDTO category = new CategoryResponseDTO(
				1L,
				"Technology",
				"Technology documents"
		);

		when(service.list()).thenReturn(List.of(category));

		List<CategoryResponseDTO> result = controller.list();

		assertEquals(1, result.size());
		assertEquals("Technology", result.get(0).name());

		verify(service).list();
	}

	@Test
	void shouldFindCategoryById() {
		CategoryResponseDTO category = new CategoryResponseDTO(
				1L,
				"Technology",
				"Technology documents"
		);

		when(service.findById(1L)).thenReturn(category);

		CategoryResponseDTO result = controller.findById(1L);

		assertEquals(1L, result.id());
		assertEquals("Technology", result.name());

		verify(service).findById(1L);
	}

	@Test
	void shouldCreateCategory() {
		CategoryRequestDTO request = new CategoryRequestDTO(
				"Technology",
				"Technology documents"
		);

		CategoryResponseDTO response = new CategoryResponseDTO(
				1L,
				"Technology",
				"Technology documents"
		);

		when(service.save(request)).thenReturn(response);

		CategoryResponseDTO result = controller.save(request);

		assertEquals(1L, result.id());
		assertEquals("Technology", result.name());
		assertEquals("Technology documents", result.description());

		verify(service).save(request);
	}

	@Test
	void shouldUpdateCategory() {
		CategoryRequestDTO request = new CategoryRequestDTO(
				"Updated Technology",
				"Updated description"
		);

		CategoryResponseDTO response = new CategoryResponseDTO(
				1L,
				"Updated Technology",
				"Updated description"
		);

		when(service.update(1L, request)).thenReturn(response);

		CategoryResponseDTO result = controller.update(1L, request);

		assertEquals(1L, result.id());
		assertEquals("Updated Technology", result.name());
		assertEquals("Updated description", result.description());

		verify(service).update(1L, request);
	}

	@Test
	void shouldDeleteCategory() {
		doNothing().when(service).delete(1L);

		controller.delete(1L);

		verify(service).delete(1L);
	}
}