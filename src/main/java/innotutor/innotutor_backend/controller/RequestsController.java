/*
MIT License

Copyright (c) 2021 InnoTutor

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package innotutor.innotutor_backend.controller;

import innotutor.innotutor_backend.DTO.card.CardDTO;
import innotutor.innotutor_backend.DTO.card.SubjectDTO;
import innotutor.innotutor_backend.security.CustomPrincipal;
import innotutor.innotutor_backend.service.CardService;
import innotutor.innotutor_backend.service.CardsListService;
import innotutor.innotutor_backend.service.SessionService;
import innotutor.innotutor_backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/requests", produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class RequestsController {

    private final CardService cardService;
    private final CardsListService cardsListService;
    private final UserService userService;
    private final SessionService sessionService;

    public RequestsController(CardService cardService, CardsListService cardsListService,
                              UserService userService, SessionService sessionService) {
        this.cardService = cardService;
        this.cardsListService = cardsListService;
        this.userService = userService;
        this.sessionService = sessionService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CardDTO>> getRequests(@AuthenticationPrincipal CustomPrincipal user) {
        List<CardDTO> requests = cardsListService.getRequests(userService.getUserId(user));
        return requests == null
                ? new ResponseEntity<>(HttpStatus.NOT_FOUND)
                : new ResponseEntity<>(requests, HttpStatus.OK);
    }

    @GetMapping(value = "/subjects", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SubjectDTO>> getAvailableRequestSubjects(@AuthenticationPrincipal CustomPrincipal user) {
        List<SubjectDTO> result = sessionService.getAvailableRequestSubjects(userService.getUserId(user));
        return result == null
                ? new ResponseEntity<>(HttpStatus.BAD_REQUEST)
                : new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping(value = "/user/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CardDTO>> getUserRequestsById(@PathVariable Long id,
                                                             @AuthenticationPrincipal CustomPrincipal user) {
        if (userService.getUserId(user).equals(id)) {
            return this.getRequests(user);
        }
        List<CardDTO> result = cardsListService.getUserRequests(id);
        return result == null
                ? new ResponseEntity<>(HttpStatus.NOT_FOUND)
                : new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CardDTO> postRequestCard(@RequestBody CardDTO cardDTO,
                                                   @AuthenticationPrincipal CustomPrincipal user) {
        if (cardDTO == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        cardDTO.setCreatorId(userService.getUserId(user));
        CardDTO result = cardService.postRequestCard(cardDTO);
        return result == null
                ? new ResponseEntity<>(HttpStatus.BAD_REQUEST)
                : new ResponseEntity<>(result, HttpStatus.CREATED);
    }

    @PutMapping(value = "/{cardId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CardDTO> putRequestCard(@PathVariable Long cardId,
                                                  @RequestBody CardDTO cardDTO,
                                                  @AuthenticationPrincipal CustomPrincipal user) {
        if (cardDTO == null || cardId == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        cardDTO.setCreatorId(userService.getUserId(user));
        cardDTO.setCardId(cardId);
        CardDTO result = cardService.putRequestCard(cardDTO);
        if (result == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(
                result,
                result.getCardId().equals(cardDTO.getCardId())
                        ? HttpStatus.OK
                        : HttpStatus.CREATED
        );
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteRequestCardById(@PathVariable Long id,
                                                   @AuthenticationPrincipal CustomPrincipal user) {
        if (id == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return cardService.deleteCardById(userService.getUserId(user), id)
                ? new ResponseEntity<>(HttpStatus.OK)
                : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
