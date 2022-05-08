import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { CookieService } from 'ngx-cookie';

import { SERVER_API_URL } from 'app/app.constants';

@Injectable({ providedIn: 'root' })
export class AuthServerProvider {
    constructor(private http: HttpClient, private cookieService: CookieService) {}

    // Get xsrf token from spring security cookie
    csrfToken: string = this.cookieService.get('XSRF-TOKEN');

    login(credentials): Observable<any> {
        const data =
            'j_username=' +
            encodeURIComponent(credentials.username) +
            '&j_password=' +
            encodeURIComponent(credentials.password) +
            '&remember-me=' +
            credentials.rememberMe +
            '&submit=Login';
        const headers = new HttpHeaders().set('Content-Type', 'application/x-www-form-urlencoded').set('X-XSRF-TOKEN', this.csrfToken);

        return this.http.post(SERVER_API_URL + 'api/authentication', data, { headers });
    }

    logout(): Observable<any> {
        // logout from the server
        return this.http.post(SERVER_API_URL + 'api/logout', {}, { observe: 'response' }).pipe(
            map((response: HttpResponse<any>) => {
                //TODO CSRF
                // to get a new csrf token call the api
                this.http.get(SERVER_API_URL + 'api/account').subscribe(() => {}, () => {});
                return response;
            })
        );
    }
}
