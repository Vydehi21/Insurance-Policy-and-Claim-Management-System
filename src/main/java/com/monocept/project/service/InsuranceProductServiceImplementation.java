package com.monocept.project.service;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.monocept.project.dto.InsuranceProductRequestDTO;
import com.monocept.project.dto.InsuranceProductResponseDTO;
import com.monocept.project.dto.PaginatedResponseDTO;
import com.monocept.project.enums.ProductType;
import com.monocept.project.exception.DuplicateResourceException;
import com.monocept.project.exception.ResourceNotFoundException;
import com.monocept.project.model.InsuranceProduct;
import com.monocept.project.repository.InsuranceProductRepository;
import com.monocept.project.util.PaginationUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsuranceProductServiceImplementation implements InsuranceProductService {

	private final InsuranceProductRepository productRepository;
	private final ModelMapper modelMapper;

	@Override
	@Transactional
	public InsuranceProductResponseDTO createProduct(InsuranceProductRequestDTO productRequestDTO) {
		log.info("Creating insurance product: {}", productRequestDTO.getProductName());

		if (productRepository.existsByProductNameIgnoreCase(productRequestDTO.getProductName())) {
			log.warn("Attempt to create duplicate product with name: {}", productRequestDTO.getProductName());
			throw new DuplicateResourceException(
					"An insurance product with name '" + productRequestDTO.getProductName() + "' already exists");
		}

		InsuranceProduct product = modelMapper.map(productRequestDTO, InsuranceProduct.class);

		product.setActiveStatus(true);

		InsuranceProduct savedProduct = productRepository.save(product);

		log.info("Insurance product created successfully with id: {}", savedProduct.getId());

		return modelMapper.map(savedProduct, InsuranceProductResponseDTO.class);
	}

	@Override
	@Transactional(readOnly = true)
	public InsuranceProductResponseDTO getProductById(Long productId) {
		log.info("Fetching insurance product with id: {}", productId);

		InsuranceProduct product = findProductById(productId);

		return modelMapper.map(product, InsuranceProductResponseDTO.class);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<InsuranceProductResponseDTO> getAllProducts(int page, 
			int size, String sortBy, String direction) {
		log.info("Fetching all insurance products");

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<InsuranceProduct> products = productRepository.findAll(pageable);

		Page<InsuranceProductResponseDTO> responsePage = products
				.map(product -> modelMapper.map(product, InsuranceProductResponseDTO.class));

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<InsuranceProductResponseDTO> getProductsByStatus(Boolean activeStatus, 
			int page, int size, String sortBy, String direction) {
		log.info("Fetching products with status: {}", activeStatus);

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<InsuranceProduct> products = productRepository.findByActiveStatus(activeStatus, pageable);

		Page<InsuranceProductResponseDTO> responsePage = products
				.map(product -> modelMapper.map(product, InsuranceProductResponseDTO.class));

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<InsuranceProductResponseDTO> getProductsByType(ProductType productType, 
			int page, int size, String sortBy, String direction) {
		log.info("Fetching products with type: {}", productType);

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<InsuranceProduct> products = productRepository.findByProductType(productType, pageable);

		Page<InsuranceProductResponseDTO> responsePage = products
				.map(product -> modelMapper.map(product, InsuranceProductResponseDTO.class));

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<InsuranceProductResponseDTO> getProductsByTypeAndStatus(ProductType productType,
			Boolean activeStatus, int page, int size, String sortBy, String direction) {
		log.info("Fetching products with type: {} and status: {}", productType, activeStatus);

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<InsuranceProduct> products = productRepository.findByProductTypeAndActiveStatus(productType, 
				activeStatus, pageable);

		Page<InsuranceProductResponseDTO> responsePage = products
				.map(product -> modelMapper.map(product, InsuranceProductResponseDTO.class));

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional(readOnly = true)
	public PaginatedResponseDTO<InsuranceProductResponseDTO> searchProductsByName(String name, int page, 
			int size, String sortBy, String direction) {
		log.info("Searching products by name: {}", name);

		Pageable pageable = PaginationUtil.createPageable(page, size, sortBy, direction);

		Page<InsuranceProduct> products = productRepository.findByProductNameContainingIgnoreCase(name, pageable);

		Page<InsuranceProductResponseDTO> responsePage = products
				.map(product -> modelMapper.map(product, InsuranceProductResponseDTO.class));

		return PaginationUtil.createPaginatedResponse(responsePage, sortBy, direction);
	}

	@Override
	@Transactional
	public InsuranceProductResponseDTO updateProduct(Long productId, InsuranceProductRequestDTO productRequestDTO) {
		log.info("Updating product with id: {}", productId);

		InsuranceProduct product = findProductById(productId);

		if (!product.getProductName().equalsIgnoreCase(productRequestDTO.getProductName())
				&& productRepository.existsByProductNameIgnoreCase(productRequestDTO.getProductName())) {
			log.warn("Attempt to rename product {} to a name already in use: {}", productId,
					productRequestDTO.getProductName());
			throw new DuplicateResourceException(
					"An insurance product with name '" + productRequestDTO.getProductName() + "' already exists");
		}

		product.setProductName(productRequestDTO.getProductName());
		product.setDescription(productRequestDTO.getDescription());
		product.setProductType(productRequestDTO.getProductType());

		InsuranceProduct updatedProduct = productRepository.save(product);

		log.info("Product updated successfully with id: {}", updatedProduct.getId());

		return modelMapper.map(updatedProduct, InsuranceProductResponseDTO.class);
	}

	@Override
	@Transactional
	public void deactivateProduct(Long productId) {
		log.info("Deactivating product with id: {}", productId);

		InsuranceProduct product = findProductById(productId);

		product.setActiveStatus(false);

		productRepository.save(product);

		log.info("Product deactivated successfully with id: {}", productId);
	}
	
	@Override
	@Transactional
	public void activateProduct(Long productId) {

	    InsuranceProduct product = findProductById(productId);

	    product.setActiveStatus(true);

	    productRepository.save(product);

	    log.info("Product activated successfully with id: {}", productId);

	}

	private InsuranceProduct findProductById(Long id) {
		return productRepository.findById(id).orElseThrow(() -> {
			log.warn("Insurance product not found with id: {}", id);
			return new ResourceNotFoundException("Insurance product not found with id: " + id);
		});
	}
}