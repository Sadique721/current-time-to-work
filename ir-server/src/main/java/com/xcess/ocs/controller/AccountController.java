package com.xcess.ocs.controller;

import com.xcess.ocs.dto.*;
import com.xcess.ocs.dto.search.AccountSearchDTO;
import com.xcess.ocs.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
//@CrossOrigin(origins = "*")
@Tag(name = "9. Accounts", description = "Endpoints for managing accounts")
@Slf4j
public class AccountController {
        @Autowired
        private AccountService accountService;

        @Operation(summary = "Create a new account", description = "Creates a new account by entering AccountDTO")
        @ApiResponses({
                @ApiResponse(responseCode = "201", description = "HTTP Status CREATED"),
                @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                @ApiResponse(responseCode = "409", description = "HTTP Status CONFLICT", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @PostMapping
        public ResponseEntity<AccountDTO> createAccount(@Valid @RequestBody AccountDTO accountDTO) {
                log.info("Received request to create account with code: {}", accountDTO.getAccountCode());
                AccountDTO created = accountService.createAccount(accountDTO);
                log.info("Successfully created account with code: {}", created.getAccountCode());
                return new ResponseEntity<>(created, HttpStatus.CREATED);
        }

        @Operation(summary = "Get paginated and filtered list of accounts",
                description = "Returns a paginated list of accounts with search functionality")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
                @ApiResponse(responseCode = "400", description = "HTTP Status BAD REQUEST",
                        content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @PostMapping("/paginated")
        public ResponseEntity<PageResponseDTO<AccountDTO>> getAccountsInPage(
                @io.swagger.v3.oas.annotations.parameters.RequestBody(
                        required = true,
                        description = "Pagination request with account search criteria",
                        content = @Content(
                                mediaType = "application/json",
                                schema = @Schema(
                                        implementation = PageRequestDTO.class
                                ),
                                examples = @ExampleObject(
                                        name = "AccountPageRequest",
                                        summary = "Example account search request",
                                        value = """
                                        {
                                            "page": 1,
                                            "pageSize": 5,
                                            "searchCriteria": {
                                                "searchTerm": "John",
                                                "accountType": "VENDOR"
                                            }
                                        }
                                        """
                                )
                        )
                )
                @Valid @RequestBody PageRequestDTO<AccountSearchDTO> pageRequestDTO) {
                log.info("REST request to get accounts in page with search criteria");

                Pageable pageable = PageRequest.of(pageRequestDTO.getPage() - 1, pageRequestDTO.getPageSize());
                PageResponseDTO<AccountDTO> response;

                if (pageRequestDTO.getSearchCriteria() != null) {
                        response = accountService.searchAccounts(pageRequestDTO.getSearchCriteria(), pageable);
                        log.info("Retrieved filtered accounts in a page");
                } else {
                        response = accountService.getAccountsInPages(pageable);
                        log.info("Retrieved all accounts in a page");
                }

                return ResponseEntity.ok(response);
        }

        @Operation(summary = "Get all accounts", description = "Returns a list of all accounts")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
                @ApiResponse(responseCode = "500", description = "HTTP Status INTERNAL SERVER ERROR", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @GetMapping
        public ResponseEntity<List<AccountDTO>> getAllAccounts() {
                List<AccountDTO> accounts = accountService.getAllAccounts();
                return ResponseEntity.ok(accounts);
        }

        @Operation(summary = "Get a specific account by ID", description = "Returns the account with the specified ID")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
                @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                @ApiResponse(responseCode = "500", description = "HTTP Status INTERNAL SERVER ERROR", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @GetMapping("/{id}")
        public ResponseEntity<AccountDTO> getAccountById(@PathVariable Long id) {
                AccountDTO dto = accountService.getAccountById(id);
                return ResponseEntity.ok(dto);
        }

        @Operation(summary = "Get accounts by partner ID", description = "Returns a list of accounts associated with the specified partner ID")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
                @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                @ApiResponse(responseCode = "500", description = "HTTP Status INTERNAL SERVER ERROR", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @GetMapping("/partner/{partnerId}")
        public ResponseEntity<List<AccountDTO>> getAccountsByPartnerId(@PathVariable Long partnerId) {
                List<AccountDTO> accounts = accountService.getAccountsByPartnerId(partnerId);
                return ResponseEntity.ok(accounts);
        }

        @Operation(summary = "Update an account by ID", description = "Updates the account with the specified ID")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
                @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                @ApiResponse(responseCode = "500", description = "HTTP Status INTERNAL SERVER ERROR", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @PutMapping("/{id}")
        public ResponseEntity<AccountDTO> updateAccount(@PathVariable Long id, @Valid @RequestBody AccountDTO accountDTO) {
                AccountDTO updated = accountService.updateAccount(id, accountDTO);
                return ResponseEntity.ok(updated);
        }

        @Operation(summary = "Delete an account by ID", description = "Deletes the account with the specified ID")
        @ApiResponses({
                @ApiResponse(responseCode = "200", description = "HTTP Status OK"),
                @ApiResponse(responseCode = "404", description = "HTTP Status NOT FOUND", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class))),
                @ApiResponse(responseCode = "500", description = "HTTP Status INTERNAL SERVER ERROR", content = @Content(schema = @Schema(implementation = ErrorResponseDTO.class)))
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
                accountService.deleteAccount(id);
                return ResponseEntity.noContent().build();
        }

        @GetMapping("/{partnerId}/accountCodes")
        public ResponseEntity<List<Map<String,Object>>> getAccountCodesByPartnerId(@PathVariable Long partnerId) {
                List<Map<String,Object>> accountCodes = accountService.getAccountCodesByPartnerId(partnerId);
                return ResponseEntity.ok(accountCodes);
        }


}