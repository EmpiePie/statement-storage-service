package za.co.statements.controller;

import org.springframework.stereotype.Controller;
import za.co.statements.service.StatementService;

import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/statements")
public class StatementUiController {

    private final StatementService statementService;

    public StatementUiController(StatementService statementService) {
        this.statementService = statementService;
    }

    @GetMapping
    public String page() {
        return "statements";
    }
}
