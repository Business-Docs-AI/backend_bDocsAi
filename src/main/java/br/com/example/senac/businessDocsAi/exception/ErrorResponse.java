package br.com.example.senac.businessDocsAi.exception;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class ErrorResponse {

    private String message;
    private Integer status;

}
