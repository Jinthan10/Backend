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

import innotutor.innotutor_backend.dto.card.CardDTO;
import innotutor.innotutor_backend.dto.enrollment.EnrollmentDTO;
import innotutor.innotutor_backend.security.CustomPrincipal;
import innotutor.innotutor_backend.service.CardEnrollService;
import innotutor.innotutor_backend.service.CardService;
import innotutor.innotutor_backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
public class CardController {

    private final UserService userService;
    private final CardService cardService;
    private final CardEnrollService cardEnrollService;

    public CardController(final UserService userService, final CardService cardService, final CardEnrollService cardEnrollService) {
        this.userService = userService;
        this.cardService = cardService;
        this.cardEnrollService = cardEnrollService;
    }

    @GetMapping(value = "/card/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CardDTO> getCard(@PathVariable final Long id, @AuthenticationPrincipal CustomPrincipal user) {
        if (id == null) {
            new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        final CardDTO card = cardService.getCardById(id, userService.getUserId(user));
        return card == null
                ? new ResponseEntity<>(HttpStatus.NOT_FOUND)
                : new ResponseEntity<>(card, HttpStatus.OK);
    }

    @PostMapping(value = "/enroll", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EnrollmentDTO> postTutorCardEnroll(@RequestBody final EnrollmentDTO enrollmentDTO,
                                                             @AuthenticationPrincipal final CustomPrincipal user) {
        if (enrollmentDTO == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        enrollmentDTO.setEnrollerId(userService.getUserId(user));
        final EnrollmentDTO result = cardEnrollService.postCardEnroll(enrollmentDTO);
        return result == null
                ? new ResponseEntity<>(HttpStatus.BAD_REQUEST)
                : new ResponseEntity<>(result, HttpStatus.CREATED);
    }
}
