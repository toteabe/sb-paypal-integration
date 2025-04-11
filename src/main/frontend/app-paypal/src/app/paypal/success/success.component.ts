import { Component, inject, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { SharedDataService } from '../shareddata.service';

@Component({
  selector: 'app-success',
  imports: [],
  templateUrl: './success.component.html',
  styleUrl: './success.component.css',
  standalone: true,
})
export class SuccessComponent {

  sharedDataService = inject(SharedDataService);

  onClose() {

    const channel = new BroadcastChannel('succes-paypal-channel');
    channel.postMessage('closeSuccessPaypal');
    window.close();

  }

}
