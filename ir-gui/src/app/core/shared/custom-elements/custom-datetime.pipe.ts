import { Pipe, PipeTransform } from '@angular/core';
import dayjs from 'src/app/core/helpers/dayjs.config';

interface IDateTimeOpt {
  givenFormat?: string;
  format?: string;
  get?: 'date' | 'time12' | 'time24' | 'datetime12' | 'datetime24' | 'UTC';
}

@Pipe({
  name: 'customDatetime',
})
export class CustomDatetimePipe implements PipeTransform {
  transform(value: string, opt: IDateTimeOpt): string {
    return customDatetime(value, opt);
  }
}

export default function customDatetime(
  value: string,
  opt: IDateTimeOpt,
): string {
  let formattedDatetime = 'Invalid Date';
  const localTz = dayjs.tz.guess();

  let dayjsDateTime;

  const { givenFormat = undefined, format = '', get = 'date' } = opt;

  const sourceTz = '';
  if (sourceTz) {

    dayjsDateTime = dayjs.tz(value, givenFormat ?? '', sourceTz).tz(localTz);
  } else {

    dayjsDateTime = dayjs(value, givenFormat);
  }

  if (!dayjsDateTime.isValid()) {
    return formattedDatetime;
  }

  if (format) {
    return dayjsDateTime.format(format);
  }

  switch (get) {
    case 'UTC':
      return dayjsDateTime.toISOString();
    case 'date':
      return dayjsDateTime.format('DD/MM/YYYY');
    case 'datetime12':
      return dayjsDateTime.format('DD/MM/YYYY hh:mm A');
    case 'datetime24':
      return dayjsDateTime.format('DD/MM/YYYY HH:mm');
    case 'time12':
      return dayjsDateTime.format('hh:mm A');
    case 'time24':
      return dayjsDateTime.format('HH:mm');
    default:
      return formattedDatetime;
  }
}
